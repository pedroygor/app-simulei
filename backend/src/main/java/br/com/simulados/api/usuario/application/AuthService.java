package br.com.simulados.api.usuario.application;

import br.com.simulados.api.shared.security.JwtService;
import br.com.simulados.api.shared.security.TokenInvalidoException;
import br.com.simulados.api.shared.security.UsuarioDetails;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final LimitadorDeTentativasDeLogin limitador;

    public record Tokens(String accessToken, String refreshToken, long expiresInSeconds) {
    }

    public Tokens login(String email, String senha) {
        // Antes de gastar bcrypt: se a conta está bloqueada, nem tenta
        limitador.verificar(email);
        try {
            // Dispara UsuarioDetailsService + comparação bcrypt. Credencial errada
            // ou usuário inativo viram AuthenticationException (tratada no handler)
            var autenticacao = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(email, senha));
            var usuario = ((UsuarioDetails) autenticacao.getPrincipal()).getUsuario();
            limitador.registrarSucesso(email);
            return gerarTokens(usuario);
        } catch (AuthenticationException e) {
            // Conta a falha e deixa a exceção seguir: quem traduz pro 401
            // genérico ("E-mail ou senha inválidos") é o handler
            limitador.registrarFalha(email);
            throw e;
        }
    }

    public Tokens refresh(String refreshToken) {
        var usuarioId = jwtService.validarRefreshToken(refreshToken);
        // Reconsulta o banco: se o usuário foi desativado depois do login,
        // o refresh para de funcionar mesmo com token ainda válido
        var usuario = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .orElseThrow(TokenInvalidoException::new);
        return gerarTokens(usuario);
    }

    private Tokens gerarTokens(Usuario usuario) {
        return new Tokens(
                jwtService.gerarAccessToken(usuario),
                // Rotação: cada refresh emite um refresh token novo
                jwtService.gerarRefreshToken(usuario),
                jwtService.accessTtlSegundos());
    }
}
