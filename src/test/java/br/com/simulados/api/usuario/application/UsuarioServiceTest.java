package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.EmailJaCadastradoException;
import br.com.simulados.api.usuario.domain.Plano;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Teste unitário puro: sem Spring, sem banco. Repository e encoder são mocks
// injetados pelo Mockito no construtor gerado pelo @RequiredArgsConstructor.
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void criaUsuarioComSenhaHasheadaEPlanoFree() {
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-forte-123")).thenReturn("$2a$hash-fake");
        // Devolve a própria entidade recebida, como o save real faria
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        var usuario = usuarioService.criar("Ana", "ana@teste.com", "senha-forte-123", Role.ALUNO);

        assertThat(usuario.getSenhaHash()).isEqualTo("$2a$hash-fake");
        assertThat(usuario.getPlano()).isEqualTo(Plano.FREE);
        assertThat(usuario.getRole()).isEqualTo(Role.ALUNO);
        assertThat(usuario.isAtivo()).isTrue();
    }

    @Test
    void lancaExcecaoQuandoEmailJaExiste() {
        when(usuarioRepository.existsByEmail("ana@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criar("Ana", "ana@teste.com", "senha-forte-123", Role.ALUNO))
                .isInstanceOf(EmailJaCadastradoException.class);

        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
