package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.ContaExistenteSemConfirmacaoDoGoogleException;
import br.com.simulados.api.usuario.domain.ContaGoogleNaoCadastradaException;
import br.com.simulados.api.usuario.domain.ContaLigadaAOutroGoogleException;
import br.com.simulados.api.usuario.domain.CredencialGoogleInvalidaException;
import br.com.simulados.api.usuario.domain.EmailGoogleNaoVerificadoException;
import br.com.simulados.api.usuario.domain.EmailJaCadastradoException;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unitário: as regras de quem entra, quem é ligado e quem é recusado. O
// verificador é um dublê; conferir o token de verdade é teste do adaptador
@ExtendWith(MockitoExtension.class)
class LoginComGoogleTest {

    private static final AuthService.Tokens TOKENS = new AuthService.Tokens("access", "refresh", 1800);
    private static final String CREDENCIAL = "id-token-do-google";

    @Mock
    private VerificadorDeTokenDoGoogle verificador;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private LoginComGoogle loginComGoogle;

    @Test
    void contaJaLigadaEntraSemGravarNada() {
        var conta = conta("ana@gmail.com", "sub-ana");
        googleDiz("sub-ana", "ana@gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-ana")).thenReturn(Optional.of(conta));
        when(authService.tokensPara(conta)).thenReturn(TOKENS);

        assertThat(loginComGoogle.entrar(CREDENCIAL)).isEqualTo(TOKENS);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void contaComSenhaEMesmoEmailVerificadoEhLigadaAoGoogle() {
        var conta = conta("ana@gmail.com", null);
        // O Google pode mandar o e-mail com outra grafia: a busca é normalizada
        googleDiz("sub-ana", "Ana@Gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-ana")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(conta));
        when(usuarioRepository.save(conta)).thenReturn(conta);
        when(authService.tokensPara(conta)).thenReturn(TOKENS);

        assertThat(loginComGoogle.entrar(CREDENCIAL)).isEqualTo(TOKENS);
        assertThat(conta.getGoogleSub()).isEqualTo("sub-ana");
        // A senha continua: a conta passa a entrar dos dois jeitos
        assertThat(conta.getSenhaHash()).isEqualTo("$2a$hash");
    }

    @Test
    void naoLigaPeloEmailQuandoOGoogleNaoConfirmouOEmail() {
        var conta = conta("ana@gmail.com", null);
        googleDiz("sub-intruso", "ana@gmail.com", false);
        when(usuarioRepository.findByGoogleSub("sub-intruso")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> loginComGoogle.entrar(CREDENCIAL))
                .isInstanceOf(ContaExistenteSemConfirmacaoDoGoogleException.class);
        assertThat(conta.getGoogleSub()).isNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoTrocaAContaGoogleJaLigada() {
        var conta = conta("ana@gmail.com", "sub-original");
        googleDiz("sub-outro", "ana@gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-outro")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> loginComGoogle.entrar(CREDENCIAL))
                .isInstanceOf(ContaLigadaAOutroGoogleException.class);
        assertThat(conta.getGoogleSub()).isEqualTo("sub-original");
    }

    @Test
    void semContaPedeOCadastro() {
        googleDiz("sub-novo", "novo@gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-novo")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("novo@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginComGoogle.entrar(CREDENCIAL))
                .isInstanceOf(ContaGoogleNaoCadastradaException.class);
    }

    @Test
    void contaDesativadaNaoEntra() {
        var conta = conta("ana@gmail.com", "sub-ana");
        conta.setAtivo(false);
        googleDiz("sub-ana", "ana@gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-ana")).thenReturn(Optional.of(conta));

        assertThatThrownBy(() -> loginComGoogle.entrar(CREDENCIAL))
                .isInstanceOf(CredencialGoogleInvalidaException.class);
        verify(authService, never()).tokensPara(any());
    }

    @Test
    void cadastroCriaAContaSoComGoogle() {
        googleDiz("sub-novo", "Novo@Gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-novo")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("novo@gmail.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authService.tokensPara(any(Usuario.class))).thenReturn(TOKENS);

        assertThat(loginComGoogle.cadastrar(CREDENCIAL, "  Novo Aluno ")).isEqualTo(TOKENS);

        var salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getNome()).isEqualTo("Novo Aluno");
        assertThat(salvo.getValue().getEmail()).isEqualTo("novo@gmail.com");
        assertThat(salvo.getValue().getGoogleSub()).isEqualTo("sub-novo");
        assertThat(salvo.getValue().getSenhaHash()).isNull();
        assertThat(salvo.getValue().getRole()).isEqualTo(Role.ALUNO);
    }

    @Test
    void cadastroRepetidoDevolveASessaoEmVezDeErro() {
        var conta = conta("novo@gmail.com", "sub-novo");
        googleDiz("sub-novo", "novo@gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-novo")).thenReturn(Optional.of(conta));
        when(authService.tokensPara(conta)).thenReturn(TOKENS);

        assertThat(loginComGoogle.cadastrar(CREDENCIAL, "Novo")).isEqualTo(TOKENS);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastroRecusaEmailQueOGoogleNaoConfirmou() {
        googleDiz("sub-novo", "novo@exemplo.com", false);
        when(usuarioRepository.findByGoogleSub("sub-novo")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginComGoogle.cadastrar(CREDENCIAL, "Novo"))
                .isInstanceOf(EmailGoogleNaoVerificadoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastroRecusaEmailDeOutraConta() {
        googleDiz("sub-novo", "ana@gmail.com", true);
        when(usuarioRepository.findByGoogleSub("sub-novo")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("ana@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> loginComGoogle.cadastrar(CREDENCIAL, "Ana"))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    private void googleDiz(String sub, String email, boolean emailVerificado) {
        when(verificador.verificar(CREDENCIAL)).thenReturn(new IdentidadeGoogle(sub, email, emailVerificado, "Nome no Google"));
    }

    private static Usuario conta(String email, String googleSub) {
        var usuario = new Usuario();
        usuario.setNome("Ana");
        usuario.setEmail(email);
        usuario.setSenhaHash("$2a$hash");
        usuario.setGoogleSub(googleSub);
        usuario.setRole(Role.ALUNO);
        return usuario;
    }
}
