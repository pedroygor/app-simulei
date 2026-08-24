package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.EmailJaCadastradoException;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.domain.UsuarioNaoEncontradoException;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Transactional
    public Usuario criar(String nome, String email, String senha, Role role) {
        // Checagem antecipada pra dar erro amigável; contra requisições
        // concorrentes, o backstop real é a constraint UNIQUE do banco
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }

        var usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
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

    @Transactional(readOnly = true)
    public Page<Usuario> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }
}
