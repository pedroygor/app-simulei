package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.TentativasDeLoginExcedidasException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Relógio controlado: dá pra atravessar a janela de 15 minutos sem esperar
class LimitadorDeTentativasDeLoginTest {

    private static final String EMAIL = "alvo@teste.com";

    /** Relógio que anda só quando mandamos. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-09-10T12:00:00Z");

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LimitadorDeTentativasDeLogin limitador = new LimitadorDeTentativasDeLogin(relogio);

    @Test
    void deixaPassarEnquantoNaoEstoura() {
        for (var i = 0; i < LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS - 1; i++) {
            limitador.registrarFalha(EMAIL);
            assertThatCode(() -> limitador.verificar(EMAIL)).doesNotThrowAnyException();
        }
    }

    @Test
    void bloqueiaDepoisDoLimite() {
        for (var i = 0; i < LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS; i++) {
            limitador.registrarFalha(EMAIL);
        }

        assertThatThrownBy(() -> limitador.verificar(EMAIL))
                .isInstanceOf(TentativasDeLoginExcedidasException.class)
                .hasMessageContaining("Tente de novo em");
    }

    @Test
    void liberaDepoisQueAJanelaPassa() {
        for (var i = 0; i < LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS; i++) {
            limitador.registrarFalha(EMAIL);
        }
        assertThatThrownBy(() -> limitador.verificar(EMAIL))
                .isInstanceOf(TentativasDeLoginExcedidasException.class);

        relogio.avancar(LimitadorDeTentativasDeLogin.JANELA.plusSeconds(1));

        assertThatCode(() -> limitador.verificar(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void loginBemSucedidoZeraAContagem() {
        for (var i = 0; i < LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS - 1; i++) {
            limitador.registrarFalha(EMAIL);
        }
        limitador.registrarSucesso(EMAIL);

        // Depois do acerto, o contador recomeça: mais uma falha não bloqueia
        limitador.registrarFalha(EMAIL);
        assertThatCode(() -> limitador.verificar(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void bloquearUmaContaNaoAfetaAsOutras() {
        // O cenário que motivou limitar por conta e não por IP: a turma toda
        // sai pelo mesmo IP do cursinho, e um aluno errando a senha não pode
        // derrubar o login dos colegas
        for (var i = 0; i < LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS; i++) {
            limitador.registrarFalha("aluno.azarado@teste.com");
        }

        assertThatThrownBy(() -> limitador.verificar("aluno.azarado@teste.com"))
                .isInstanceOf(TentativasDeLoginExcedidasException.class);
        assertThatCode(() -> limitador.verificar("colega@teste.com")).doesNotThrowAnyException();
    }

    @Test
    void naoDeixaDriblarTrocandoMaiusculas() {
        for (var i = 0; i < LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS; i++) {
            limitador.registrarFalha("Alvo@Teste.com");
        }

        assertThatThrownBy(() -> limitador.verificar("alvo@teste.com  "))
                .isInstanceOf(TentativasDeLoginExcedidasException.class);
    }

    @Test
    void mensagemDizQuantoFaltaEsperar() {
        for (var i = 0; i < LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS; i++) {
            limitador.registrarFalha(EMAIL);
        }
        relogio.avancar(Duration.ofMinutes(10));

        assertThatThrownBy(() -> limitador.verificar(EMAIL))
                .hasMessageContaining("5 minuto");
    }

    @Test
    void contaNovaNaoNasceBloqueada() {
        assertThatCode(() -> limitador.verificar("nunca.tentou@teste.com")).doesNotThrowAnyException();
        assertThat(LimitadorDeTentativasDeLogin.MAXIMO_DE_FALHAS).isEqualTo(5);
    }
}
