package br.com.simulados.api.simulado.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.ItemDoHistorico;
import br.com.simulados.api.simulado.domain.Placar;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.SituacaoDaProva;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Banco real: o GROUP BY com LEFT JOINs é o que se testa. Três provas da mesma
 * aluna, datas fixadas por UPDATE (criadas no mesmo instante, a ordem seria sorte):
 *   "Antiga" (01/10) entregue: q1 certa, q2 errada, q3 em branco
 *   "Do meio" (02/10) em andamento: q1 certa
 *   "Nova"   (03/10) não iniciada
 * Gabarito de toda questão: A.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class HistoricoDeProvasTest {

    @Autowired
    private HistoricoDeProvas historico;

    @Autowired
    private ExecucaoDaProva execucao;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID aluna;
    private UUID antiga;
    private UUID doMeio;
    private UUID nova;
    private final List<Questao> q = new ArrayList<>();

    @BeforeEach
    void setUp() {
        aluna = usuario("aluna@exemplo.com");
        for (int n = 1; n <= 3; n++) q.add(questaoRepository.save(questao(n)));
        var ids = q.stream().map(Questao::getId).toList();
        antiga = simulado("Antiga", ids, "2026-10-01T10:00:00Z");
        doMeio = simulado("Do meio", ids, "2026-10-02T10:00:00Z");
        nova = simulado("Nova", ids, "2026-10-03T10:00:00Z");
        // A prova de outra pessoa não pode aparecer
        var outra = usuario("outra@exemplo.com");
        simuladoRepository.save(new Simulado(outra, "Alheia", null, ids));

        var entregue = execucao.iniciar(antiga, aluna).tentativa().getId();
        execucao.responder(entregue, aluna, q.get(0).getId(), alternativa(q.get(0), "A"));
        execucao.responder(entregue, aluna, q.get(1).getId(), alternativa(q.get(1), "B"));
        execucao.finalizar(entregue, aluna);

        var aberta = execucao.iniciar(doMeio, aluna).tentativa().getId();
        execucao.responder(aberta, aluna, q.get(0).getId(), alternativa(q.get(0), "A"));
        sincronizar();
    }

    @Test
    void listaDaMaisNovaPraMaisVelhaComSituacaoEPlacar() {
        var pagina = historico.listar(aluna, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(ItemDoHistorico::titulo, ItemDoHistorico::situacao)
                .containsExactly(
                        tuple("Nova", SituacaoDaProva.NAO_INICIADA),
                        tuple("Do meio", SituacaoDaProva.EM_ANDAMENTO),
                        tuple("Antiga", SituacaoDaProva.ENTREGUE));
        var entregue = pagina.getContent().get(2);
        assertThat(entregue.placarSeEntregue()).isEqualTo(new Placar(3, 2, 1));
        assertThat(entregue.finalizadoEm()).isNotNull();
        var naoIniciada = pagina.getContent().get(0);
        assertThat(naoIniciada.tentativaId()).isNull();
        assertThat(naoIniciada.questoes()).isEqualTo(3);
        assertThat(naoIniciada.respondidas()).isZero();
    }

    @Test
    void provaEmAndamentoNaoRevelaAcertos() {
        var doMeio = historico.listar(aluna, PageRequest.of(0, 20)).getContent().get(1);

        // Respondeu 1 (e acertou), mas o histórico não pode dizer: seria o gabarito
        assertThat(doMeio.respondidas()).isEqualTo(1);
        assertThat(doMeio.placarSeEntregue()).isNull();
    }

    @Test
    void paginaPelosSimuladosEIgnoraOSortDoCliente() {
        var primeira = historico.listar(aluna, PageRequest.of(0, 2, Sort.by("titulo")));
        var segunda = historico.listar(aluna, PageRequest.of(1, 2));

        // Por título a ordem seria Antiga, Do meio, Nova; a data manda
        assertThat(primeira.getContent()).extracting(ItemDoHistorico::titulo).containsExactly("Nova", "Do meio");
        assertThat(primeira.getTotalElements()).isEqualTo(3);
        assertThat(primeira.getTotalPages()).isEqualTo(2);
        assertThat(segunda.getContent()).extracting(ItemDoHistorico::titulo).containsExactly("Antiga");
    }

    @Test
    void questaoRetiradaSaiDaContagemSemDerrubarAProva() {
        // q3 (a em branco da "Antiga") é anulada; depois, todas da "Nova" somem
        anular(q.get(2).getId());
        var antes = historico.listar(aluna, PageRequest.of(0, 20)).getContent();
        assertThat(antes.get(2).placarSeEntregue()).isEqualTo(new Placar(2, 2, 1));

        anular(q.get(0).getId());
        anular(q.get(1).getId());
        var depois = historico.listar(aluna, PageRequest.of(0, 20)).getContent();

        // Sem nenhuma questão disponível, a prova continua na lista, com 0
        assertThat(depois).hasSize(3);
        assertThat(depois.get(0).questoes()).isZero();
    }

    @Test
    void duasQueriesQualquerQueSejaAPagina() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        // Página menor que o total: força a query de contagem (o Spring Data a
        // pula quando tudo cabe numa página)
        historico.listar(aluna, PageRequest.of(0, 2));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    // --- apoio ---

    private UUID simulado(String titulo, List<Long> questoes, String criadoEm) {
        var id = simuladoRepository.save(new Simulado(aluna, titulo, null, questoes)).getId();
        sincronizar();
        entityManager.createNativeQuery("UPDATE simulados SET criado_em = CAST(:quando AS timestamptz) WHERE id = :id")
                .setParameter("quando", criadoEm)
                .setParameter("id", id)
                .executeUpdate();
        return id;
    }

    private void anular(Long questao) {
        entityManager.createNativeQuery("UPDATE questoes SET anulada = true, gabarito = NULL WHERE id = :id")
                .setParameter("id", questao)
                .executeUpdate();
        sincronizar();
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
        questao.setAno(2022);
        questao.setNumero(numero + 135);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2022/" + (numero + 135));
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
