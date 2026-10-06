package br.com.simulados.api.simulado.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.Resposta;
import br.com.simulados.api.simulado.domain.RespostaInvalidaException;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.SimuladoNaoEncontradoException;
import br.com.simulados.api.simulado.domain.StatusDaTentativa;
import br.com.simulados.api.simulado.domain.TentativaJaFinalizadaException;
import br.com.simulados.api.simulado.domain.TentativaNaoEncontradaException;
import br.com.simulados.api.simulado.infrastructure.RespostaRepository;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real: trava FOR UPDATE, INSERT ... ON CONFLICT e a query que valida e
// corrige a resposta só têm sentido contra o Postgres. Gabarito de toda questão: A
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ExecucaoDaProvaTest {

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
    private EntityManager entityManager;

    private UUID aluna;
    private UUID outra;
    private UUID simulado;
    private Questao q1;
    private Questao q2;
    private Questao foraDaProva;

    @BeforeEach
    void setUp() {
        aluna = usuario("aluna@exemplo.com");
        outra = usuario("outra@exemplo.com");
        q1 = questaoRepository.save(questao(1));
        q2 = questaoRepository.save(questao(2));
        foraDaProva = questaoRepository.save(questao(3));
        simulado = simuladoRepository.save(new Simulado(aluna, "Treino", null, List.of(q1.getId(), q2.getId()))).getId();
        sincronizar();
    }

    // --- iniciar ---

    @Test
    void iniciarDuasVezesDevolveAMesmaTentativa() {
        var primeira = execucao.iniciar(simulado, aluna);
        var segunda = execucao.iniciar(simulado, aluna);

        assertThat(primeira.nova()).isTrue();
        assertThat(segunda.nova()).isFalse();
        assertThat(segunda.tentativa().getId()).isEqualTo(primeira.tentativa().getId());
        assertThat(segunda.tentativa().getStatus()).isEqualTo(StatusDaTentativa.EM_ANDAMENTO);
        assertThat(tentativaRepository.count()).isEqualTo(1);
    }

    @Test
    void simuladoAlheioOuInexistenteNaoIniciaNada() {
        assertThatThrownBy(() -> execucao.iniciar(simulado, outra))
                .isInstanceOf(SimuladoNaoEncontradoException.class);
        assertThatThrownBy(() -> execucao.iniciar(UUID.randomUUID(), aluna))
                .isInstanceOf(SimuladoNaoEncontradoException.class);
        assertThat(tentativaRepository.count()).isZero();
    }

    @Test
    void iniciarCustaDuasQueries() {
        var statistics = estatisticas();

        execucao.iniciar(simulado, aluna);

        // INSERT ... ON CONFLICT + SELECT da tentativa
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    // --- responder ---

    @Test
    void respostaGuardaSeEstaCertaETrocarRecalcula() {
        var tentativa = iniciar();

        execucao.responder(tentativa, aluna, q1.getId(), alternativa(q1, "A"));
        assertThat(respostas(tentativa)).singleElement().extracting(Resposta::isCorreta).isEqualTo(true);

        execucao.responder(tentativa, aluna, q1.getId(), alternativa(q1, "D"));
        assertThat(respostas(tentativa)).singleElement().satisfies(r -> {
            assertThat(r.getAlternativaId()).isEqualTo(alternativa(q1, "D"));
            assertThat(r.isCorreta()).isFalse();
        });
    }

    @Test
    void responderCustaTresQueries() {
        var tentativa = iniciar();
        var statistics = estatisticas();

        execucao.responder(tentativa, aluna, q1.getId(), alternativa(q1, "B"));

        // tentativa (FOR UPDATE) + validação/correção + upsert
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void recusaQuestaoDeForaDaProvaEAlternativaDeOutraQuestao() {
        var tentativa = iniciar();

        assertThatThrownBy(() -> execucao.responder(tentativa, aluna, foraDaProva.getId(), alternativa(foraDaProva, "A")))
                .isInstanceOf(RespostaInvalidaException.class);
        assertThatThrownBy(() -> execucao.responder(tentativa, aluna, q1.getId(), alternativa(q2, "A")))
                .isInstanceOf(RespostaInvalidaException.class);
        assertThat(respostas(tentativa)).isEmpty();
    }

    @Test
    void questaoAnuladaDepoisDaMontagemNaoAceitaResposta() {
        var tentativa = iniciar();
        // A conferência do INEP pode anular uma questão que já estava num simulado;
        // ela sai da prova (decidido em 02/10/2026), então responder é inválido
        entityManager.createNativeQuery("UPDATE questoes SET anulada = true, gabarito = NULL WHERE id = :id")
                .setParameter("id", q1.getId())
                .executeUpdate();

        assertThatThrownBy(() -> execucao.responder(tentativa, aluna, q1.getId(), alternativa(q1, "A")))
                .isInstanceOf(RespostaInvalidaException.class);
        assertThat(respostas(tentativa)).isEmpty();
    }

    // --- desmarcar ---

    @Test
    void desmarcarDeixaEmBrancoEDesmarcarDeNovoNaoEhErro() {
        var tentativa = iniciar();
        execucao.responder(tentativa, aluna, q1.getId(), alternativa(q1, "A"));

        execucao.desmarcar(tentativa, aluna, q1.getId());
        execucao.desmarcar(tentativa, aluna, q1.getId());

        assertThat(respostas(tentativa)).isEmpty();
    }

    // --- finalizar ---

    @Test
    void provaFinalizadaNaoAceitaMaisNada() {
        var tentativa = iniciar();
        execucao.responder(tentativa, aluna, q1.getId(), alternativa(q1, "A"));

        var finalizada = execucao.finalizar(tentativa, aluna);
        sincronizar();

        assertThat(finalizada.getStatus()).isEqualTo(StatusDaTentativa.FINALIZADA);
        assertThat(tentativaRepository.findById(tentativa).orElseThrow().getFinalizadoEm()).isNotNull();
        assertThatThrownBy(() -> execucao.responder(tentativa, aluna, q2.getId(), alternativa(q2, "A")))
                .isInstanceOf(TentativaJaFinalizadaException.class);
        assertThatThrownBy(() -> execucao.desmarcar(tentativa, aluna, q1.getId()))
                .isInstanceOf(TentativaJaFinalizadaException.class);
        assertThatThrownBy(() -> execucao.finalizar(tentativa, aluna))
                .isInstanceOf(TentativaJaFinalizadaException.class);
        assertThat(respostas(tentativa)).hasSize(1);
        // Iniciar de novo devolve a entregue, não abre outra
        assertThat(execucao.iniciar(simulado, aluna).tentativa().getId()).isEqualTo(tentativa);
    }

    // --- dono ---

    @Test
    void tentativaAlheiaEhComoSeNaoExistisse() {
        var tentativa = iniciar();

        assertThatThrownBy(() -> execucao.responder(tentativa, outra, q1.getId(), alternativa(q1, "A")))
                .isInstanceOf(TentativaNaoEncontradaException.class);
        assertThatThrownBy(() -> execucao.desmarcar(tentativa, outra, q1.getId()))
                .isInstanceOf(TentativaNaoEncontradaException.class);
        assertThatThrownBy(() -> execucao.finalizar(tentativa, outra))
                .isInstanceOf(TentativaNaoEncontradaException.class);
        assertThat(respostas(tentativa)).isEmpty();
    }

    // --- apoio ---

    private UUID iniciar() {
        var id = execucao.iniciar(simulado, aluna).tentativa().getId();
        sincronizar();
        return id;
    }

    private List<Resposta> respostas(UUID tentativa) {
        sincronizar();
        return respostaRepository.findByTentativaId(tentativa);
    }

    private Statistics estatisticas() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();
        return statistics;
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private UUID usuario(String email) {
        var usuario = new Usuario();
        usuario.setNome("Aluna");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        return usuarioRepository.save(usuario).getId();
    }

    private static Long alternativa(Questao questao, String letra) {
        return questao.getAlternativas().stream()
                .filter(a -> a.getLetra().equals(letra))
                .findFirst().orElseThrow()
                .getId();
    }

    private static Questao questao(int numero) {
        var questao = new Questao();
        questao.setAno(2020);
        questao.setNumero(numero + 135);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2020/" + (numero + 135));
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
