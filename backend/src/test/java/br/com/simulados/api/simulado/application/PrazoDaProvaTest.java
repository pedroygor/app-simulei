package br.com.simulados.api.simulado.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.StatusDaTentativa;
import br.com.simulados.api.simulado.domain.TempoEsgotadoException;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O tempo-limite imposto pelo servidor (3d). Banco real: o prazo é calculado
 * pelo INSERT nativo e o encerramento é um UPDATE nativo. Pra "o tempo passou",
 * o prazo é fixado por UPDATE com um Instant do Clock da aplicação — sem
 * depender de o relógio do Postgres bater com o da JVM.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PrazoDaProvaTest {

    @Autowired
    private ExecucaoDaProva execucao;

    @Autowired
    private EncerramentoDeProvasVencidas encerramento;

    @Autowired
    private TentativaRepository tentativaRepository;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private UUID aluna;
    private Questao questao;

    @BeforeEach
    void setUp() {
        var usuario = new Usuario();
        usuario.setNome("Aluna");
        usuario.setEmail("aluna@exemplo.com");
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        aluna = usuarioRepository.save(usuario).getId();
        questao = questaoRepository.save(questao());
        sincronizar();
    }

    @Test
    void iniciarGravaOPrazoPeloTempoDoSimulado() {
        var comTempo = iniciar(3600);
        var semTempo = iniciar(null);

        var tentativa = tentativaRepository.findById(comTempo).orElseThrow();
        assertThat(Duration.between(tentativa.getIniciadoEm(), tentativa.getPrazo())).isEqualTo(Duration.ofHours(1));
        assertThat(tentativaRepository.findById(semTempo).orElseThrow().getPrazo()).isNull();
    }

    @Test
    void depoisDoPrazoNaoResponderNemDesmarca() {
        var tentativa = iniciar(3600);
        fixarPrazo(tentativa, agora().minus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> execucao.responder(tentativa, aluna, questao.getId(), alternativa("A")))
                .isInstanceOf(TempoEsgotadoException.class)
                .hasMessage("O tempo da prova acabou");
        assertThatThrownBy(() -> execucao.desmarcar(tentativa, aluna, questao.getId()))
                .isInstanceOf(TempoEsgotadoException.class);
    }

    @Test
    void respostaDoUltimoSegundoEntraNaTolerancia() {
        var tentativa = iniciar(3600);
        // Venceu há 5 s, dentro dos 10 s de folga: a resposta estava a caminho
        fixarPrazo(tentativa, agora().minus(Duration.ofSeconds(5)));

        execucao.responder(tentativa, aluna, questao.getId(), alternativa("A"));
    }

    @Test
    void entregarDepoisDoPrazoPodeEGravaOPrazoComoHoraDaEntrega() {
        var tentativa = iniciar(3600);
        var prazo = agora().minus(Duration.ofMinutes(30));
        fixarPrazo(tentativa, prazo);

        execucao.finalizar(tentativa, aluna);
        sincronizar();

        var entregue = tentativaRepository.findById(tentativa).orElseThrow();
        assertThat(entregue.getStatus()).isEqualTo(StatusDaTentativa.FINALIZADA);
        assertThat(entregue.getFinalizadoEm()).isEqualTo(prazo);
    }

    @Test
    void encerramentoEntregaSoAsVencidasForaDaTolerancia() {
        var vencida = iniciar(3600);
        var naTolerancia = iniciar(3600);
        var noPrazo = iniciar(3600);
        var semTempo = iniciar(null);
        var prazoDaVencida = agora().minus(Duration.ofHours(1));
        fixarPrazo(vencida, prazoDaVencida);
        fixarPrazo(naTolerancia, agora().minus(Duration.ofSeconds(5)));

        assertThat(encerramento.encerrar()).isEqualTo(1);
        sincronizar();

        var entregue = tentativaRepository.findById(vencida).orElseThrow();
        assertThat(entregue.getStatus()).isEqualTo(StatusDaTentativa.FINALIZADA);
        // A prova acabou no prazo, não quando o job passou
        assertThat(entregue.getFinalizadoEm()).isEqualTo(prazoDaVencida);
        assertThat(List.of(naTolerancia, noPrazo, semTempo))
                .allSatisfy(id -> assertThat(tentativaRepository.findById(id).orElseThrow().finalizada()).isFalse());
        // Rodar de novo não acha mais nada (idempotente)
        assertThat(encerramento.encerrar()).isZero();
    }

    // --- apoio ---

    /** Um simulado novo (a regra é uma tentativa por simulado e aluno) e a tentativa nele. */
    private UUID iniciar(Integer tempoLimiteSeg) {
        var simulado = simuladoRepository.save(new Simulado(aluna, "Prova", tempoLimiteSeg, List.of(questao.getId()))).getId();
        sincronizar();
        var tentativa = execucao.iniciar(simulado, aluna).tentativa().getId();
        sincronizar();
        return tentativa;
    }

    /** Move a prova no tempo: iniciada 1 h antes do prazo pedido (respeita o CHECK). */
    private void fixarPrazo(UUID tentativa, Instant prazo) {
        entityManager.createNativeQuery("UPDATE tentativas SET iniciado_em = :inicio, prazo = :prazo WHERE id = :id")
                .setParameter("inicio", prazo.minus(Duration.ofHours(1)))
                .setParameter("prazo", prazo)
                .setParameter("id", tentativa)
                .executeUpdate();
        sincronizar();
    }

    // Truncado: o Postgres guarda microssegundos, e a comparação com o relido
    // falharia pelos nanossegundos que a JVM tem e o banco não
    private Instant agora() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private Long alternativa(String letra) {
        return questao.getAlternativas().stream()
                .filter(a -> a.getLetra().equals(letra))
                .findFirst().orElseThrow()
                .getId();
    }

    private static Questao questao() {
        var questao = new Questao();
        questao.setAno(2018);
        questao.setNumero(150);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2018/150");
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
