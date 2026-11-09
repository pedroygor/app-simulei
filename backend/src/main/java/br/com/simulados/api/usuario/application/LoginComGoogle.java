package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.ContaExistenteSemConfirmacaoDoGoogleException;
import br.com.simulados.api.usuario.domain.ContaGoogleNaoCadastradaException;
import br.com.simulados.api.usuario.domain.ContaLigadaAOutroGoogleException;
import br.com.simulados.api.usuario.domain.CredencialGoogleInvalidaException;
import br.com.simulados.api.usuario.domain.EmailGoogleNaoVerificadoException;
import br.com.simulados.api.usuario.domain.EmailJaCadastradoException;
import br.com.simulados.api.usuario.domain.Emails;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Entrar e cadastrar com a conta Google. O ID token que o botão do Google
 * entrega ao front é a prova de identidade nos dois casos; depois de conferido,
 * a sessão é a mesma do login por senha (AuthService.tokensPara).
 *
 * SEM @Transactional, pelo mesmo motivo do cadastro por senha: o primeiro passo
 * é conferir o token, e isso pode ser uma chamada de rede ao Google (buscar as
 * chaves). Dentro de uma transação, essa espera seguraria uma conexão do pool.
 * Cada caminho tem uma escrita só, e o save já roda na própria transação. Contra
 * duas requisições simultâneas, o backstop é a UNIQUE do banco (409).
 */
@Service
@RequiredArgsConstructor
public class LoginComGoogle {

    private final VerificadorDeTokenDoGoogle verificador;
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;

    /**
     * @throws ContaGoogleNaoCadastradaException não há conta: o front abre
     *                                           "completar cadastro"
     */
    public AuthService.Tokens entrar(String credencial) {
        var identidade = verificador.verificar(credencial);

        // 1. Conta já ligada a esta pessoa do Google. O "sub" vem antes do e-mail
        // porque é ele que não muda: quem trocou o e-mail no Google continua entrando
        var ligada = usuarioRepository.findByGoogleSub(identidade.sub());
        if (ligada.isPresent()) {
            return authService.tokensPara(ativa(ligada.get()));
        }

        // 2. Conta criada com senha e o mesmo e-mail: liga ao Google, se for seguro
        var email = Emails.normalizar(identidade.email());
        var mesmoEmail = usuarioRepository.findByEmail(email)
                .orElseThrow(ContaGoogleNaoCadastradaException::new);
        if (!identidade.emailVerificado()) {
            throw new ContaExistenteSemConfirmacaoDoGoogleException();
        }
        if (mesmoEmail.getGoogleSub() != null) {
            throw new ContaLigadaAOutroGoogleException();
        }
        mesmoEmail.setGoogleSub(identidade.sub());
        return authService.tokensPara(ativa(usuarioRepository.save(mesmoEmail)));
    }

    /**
     * Cria a conta pelo Google, sem senha. O nome vem confirmado pela pessoa
     * (o front sugere o do Google). Quando a LGPD entrar, a data de nascimento e
     * o aceite entram aqui, sem mexer no fluxo do Google.
     */
    public AuthService.Tokens cadastrar(String credencial, String nome) {
        var identidade = verificador.verificar(credencial);

        // Idempotente: dois toques em "criar conta" não podem dar erro no
        // segundo, e o próprio token prova quem é
        var existente = usuarioRepository.findByGoogleSub(identidade.sub());
        if (existente.isPresent()) {
            return authService.tokensPara(ativa(existente.get()));
        }

        if (!identidade.emailVerificado()) {
            throw new EmailGoogleNaoVerificadoException();
        }
        var email = Emails.normalizar(identidade.email());
        if (usuarioRepository.existsByEmail(email)) {
            // Chegar aqui com o e-mail já usado é corrida ou cliente fora do
            // fluxo: o "entrar" teria ligado a conta. Recusa como o cadastro por senha
            throw new EmailJaCadastradoException(email);
        }

        var usuario = new Usuario();
        usuario.setNome(nome.strip());
        usuario.setEmail(email);
        usuario.setGoogleSub(identidade.sub());
        // senhaHash fica nulo: a conta entra só pelo Google (CHECK da V15)
        usuario.setRole(Role.ALUNO);
        return authService.tokensPara(usuarioRepository.save(usuario));
    }

    // Conta desativada não entra, como no login por senha. Mesma resposta
    // genérica de token recusado: não revela que a conta existe
    private static Usuario ativa(Usuario usuario) {
        if (!usuario.isAtivo()) {
            throw new CredencialGoogleInvalidaException();
        }
        return usuario;
    }
}
