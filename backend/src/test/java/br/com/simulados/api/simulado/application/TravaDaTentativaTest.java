package br.com.simulados.api.simulado.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.TentativaJaFinalizadaException;
import br.com.simulados.api.simulado.infrastructure.RespostaRepository;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A trava FOR UPDATE de ExecucaoDaProva, provada com duas transações de verdade.
 *
 * SEM @Transactional na classe, de propósito: com ela, o teste inteiro seria uma
 * transação só, desfeita no fim, e não haveria concorrência nenhuma pra medir.
 * Aqui os dados são gravados com commit e apagados no @AfterEach — por isso o
 * teste limpa o que criou, ao contrário dos outros.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TravaDaTentativaTest {

    @Autowired
    private ExecucaoDaProva execucao;

    @Autowired
    private TentativaRepository tentativaRepository;

    @Autowired
    private RespostaRepository respostaRepository;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID aluna;
    private UUID simulado;
    private Questao questao;
    private UUID tentativa;

    @BeforeEach
    void setUp() {
        // Cada save() aqui é a própria transação do repository: dado com commit
        var usuario = new Usuario();
        usuario.setNome("Aluna");
        usuario.setEmail("trava-" + UUID.randomUUID() + "@exemplo.com");
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        aluna = usuarioRepository.save(usuario).getId();
        questao = questaoRepository.save(questao());
        simulado = simuladoRepository.save(new Simulado(aluna, "Trava", null, List.of(questao.getId()))).getId();
        tentativa = execucao.iniciar(simulado, aluna).tentativa().getId();
    }

    @AfterEach
    void limpar() {
        // Ordem das FKs: tentativa (leva as respostas por CASCADE) → simulado →
        // questão (leva as alternativas) → usuário
        jdbc.update("DELETE FROM tentativas WHERE simulado_id = ?", simulado);
        jdbc.update("DELETE FROM simulados WHERE id = ?", simulado);
        jdbc.update("DELETE FROM questoes WHERE id = ?", questao.getId());
        jdbc.update("DELETE FROM usuarios WHERE id = ?", aluna);
    }

    @Test
    void responderEsperaOFinalizarDeOutraAbaEVeAProvaEntregue() throws Exception {
        var travou = new CountDownLatch(1);
        var podeTerminar = new CountDownLatch(1);
        var transacao = new TransactionTemplate(transactionManager);

        // Aba A: trava a tentativa e finaliza, mas segura o commit
        var finalizar = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(status -> {
            var t = tentativaRepository.buscarDoAlunoParaEscrita(tentativa, aluna).orElseThrow();
            t.finalizar(Instant.now());
            tentativaRepository.flush();
            travou.countDown();
            aguardar(podeTerminar);
        }));
        assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();

        // Aba B: responde enquanto A ainda não fez commit
        var alternativa = questao.getAlternativas().getFirst().getId();
        var responder = CompletableFuture.runAsync(() -> execucao.responder(tentativa, aluna, questao.getId(), alternativa));

        // Sem a trava, B leria EM_ANDAMENTO (A não fez commit) e terminaria já
        Thread.sleep(700);
        assertThat(responder).as("responder deveria esperar a trava do finalizar").isNotDone();

        podeTerminar.countDown();
        finalizar.get(10, TimeUnit.SECONDS);

        // Depois do commit de A, B acorda, relê a linha e vê a prova entregue
        assertThatThrownBy(() -> responder.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(TentativaJaFinalizadaException.class);
        assertThat(respostaRepository.findByTentativaId(tentativa)).isEmpty();
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("o teste não liberou a transação a tempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static Questao questao() {
        var questao = new Questao();
        questao.setAno(2019);
        // Número fora da faixa usada pelos outros testes (a chave natural é única)
        questao.setNumero(178);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2019/178");
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
