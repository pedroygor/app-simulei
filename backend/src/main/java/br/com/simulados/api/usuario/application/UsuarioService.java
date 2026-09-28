package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.EmailJaCadastradoException;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.domain.UsuarioNaoEncontradoException;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Recebe os dados crus (não o Request do pacote api): a camada de aplicação
    // não deve depender de tipos da camada de cima
    /**
     * SEM @Transactional, de propósito — e não por esquecimento.
     *
     * Duas razões. Primeira: aqui há uma única escrita, e o `save` do Spring
     * Data já roda na própria transação. Envolver tudo numa transação externa
     * não acrescenta atomicidade nenhuma.
     *
     * Segunda, a que motivou a mudança: o bcrypt custa ~170ms de CPU por
     * design (é o que o torna caro de quebrar). Dentro de uma transação, esse
     * tempo todo seguraria uma conexão do pool sem usá-la — desperdiçando o
     * recurso mais escasso da aplicação.
     *
     * A ordem das operações também importa: checar o e-mail ANTES de hashear
     * evita queimar CPU em cadastro duplicado (um teste guarda essa ordem).
     *
     * A checagem de e-mail existente é conveniência (mensagem amigável). O
     * backstop contra a corrida continua sendo a UNIQUE do banco, traduzida
     * pra 409 no GlobalExceptionHandler.
     *
     * Cuidado se um dia isto virar duas escritas: NÃO resolva extraindo um
     * método @Transactional privado nesta mesma classe. O Spring usa proxy, e
     * chamada interna não passa por ele — a anotação seria ignorada em
     * silêncio. Nesse caso, o método público é que leva a anotação.
     */
    public Usuario criar(String nome, String email, String senha, Role role) {
        // Checagem antecipada pra dar erro amigável; contra requisições
        // concorrentes, o backstop real é a constraint UNIQUE do banco.
        //
        // Vem ANTES do hash de propósito: hashear primeiro gastaria ~170ms de
        // CPU em toda tentativa com e-mail repetido, o que é um jeito barato
        // de derrubar o servidor mandando cadastros duplicados em massa.
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }

        // Hash fora de qualquer transação: 170ms segurando conexão do pool
        // seria desperdiçar o recurso mais escasso
        var senhaHash = passwordEncoder.encode(senha);

        var usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(senhaHash);
        usuario.setRole(role);
        // plano FREE e ativo=true são defaults da própria entidade
        return usuarioRepository.save(usuario);
    }

    // readOnly: dica pro Hibernate/driver de que não haverá escrita (menos overhead)
    @Transactional(readOnly = true)
    public Usuario buscarPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}
