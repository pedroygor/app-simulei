package br.com.simulados.api.desempenho.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.application.ExecucaoDaProva;
import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Banco real: a agregação sobre várias tentativas é o que se testa. Gabarito de toda
 * questão: A. q1–q3 de probabilidade, q4–q5 de ecologia.
 *
 *   Prova 1 (entregue): q1 A certa, q2 B errada, q3 em branco, q4 A certa, q5 em branco
 *   Prova 2 (entregue): q1 B errada, q4 A certa (o resto em branco)
 *   Prova 3 (EM ANDAMENTO): q1 A — não pode contar
 *   Prova da outra aluna (entregue): tudo A — não pode contar
 *
 * Esperado: probabilidade 6 questões, 3 respondidas, 1 certa; ecologia 4, 2, 2.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DesempenhoRepositoryTest {

    @Autowired
    private DesempenhoRepository repository;

    @Autowired
    private ExecucaoDaProva execucao;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private AssuntoRepository assuntoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID aluna;
    private Map<String, Assunto> assuntos;
    private final List<Questao> q = new ArrayList<>();

    @BeforeEach
    void setUp() {
        aluna = usuario("aluna@exemplo.com");
        var outra = usuario("outra@exemplo.com");
        assuntos = assuntoRepository.findAll().stream().collect(Collectors.toMap(Assunto::getSlug, a -> a));
        for (int n = 1; n <= 3; n++) q.add(questaoRepository.save(questao(n, assuntos.get("probabilidade"))));
        for (int n = 4; n <= 5; n++) q.add(questaoRepository.save(questao(n, assuntos.get("ecologia"))));

        prova(aluna, true, "A", "B", null, "A", null);
        prova(aluna, true, "B", null, null, "A", null);
        prova(aluna, false, "A", null, null, null, null);
        prova(outra, true, "A", "A", "A", "A", "A");
        sincronizar();
    }

    @Test
    void somaSoAsProvasEntreguesDaAluna() {
        assertThat(repository.linhasDoAluno(aluna))
                .extracting(LinhaDoResultado::assuntoSlug, LinhaDoResultado::questoes,
                        LinhaDoResultado::respondidas, LinhaDoResultado::acertos)
                .containsExactlyInAnyOrder(
                        tuple("probabilidade", 6L, 3L, 1L),
                        tuple("ecologia", 4L, 2L, 2L));
        assertThat(repository.provasEntregues(aluna)).isEqualTo(2);
    }

    @Test
    void questaoAnuladaSaiDoDiagnostico() {
        // q2 era o único erro de probabilidade na prova 1
        executar("UPDATE questoes SET anulada = true, gabarito = NULL WHERE id = " + q.get(1).getId());

        assertThat(repository.linhasDoAluno(aluna))
                .filteredOn(l -> "probabilidade".equals(l.assuntoSlug()))
                .extracting(LinhaDoResultado::questoes, LinhaDoResultado::respondidas, LinhaDoResultado::acertos)
                .containsExactly(tuple(4L, 2L, 1L));
    }

    @Test
    void reclassificarAQuestaoCorrigeOHistorico() {
        // q1 passa de probabilidade pra estatística: as duas respostas a ela mudam de assunto
        var estatistica = assuntos.get("estatistica");
        executar("UPDATE questoes SET assunto_id = " + estatistica.getId() + " WHERE id = " + q.get(0).getId());

        assertThat(repository.linhasDoAluno(aluna))
                .filteredOn(l -> l.area() == estatistica.getArea())
                .extracting(LinhaDoResultado::assuntoSlug, LinhaDoResultado::questoes,
                        LinhaDoResultado::respondidas, LinhaDoResultado::acertos)
                .containsExactlyInAnyOrder(
                        tuple("estatistica", 2L, 2L, 1L),
                        tuple("probabilidade", 4L, 1L, 0L));
    }

    @Test
    void alunoSemProvaEntregueNaoTemLinha() {
        var nova = usuario("nova@exemplo.com");

        assertThat(repository.linhasDoAluno(nova)).isEmpty();
        assertThat(repository.provasEntregues(nova)).isZero();
    }

    // --- apoio ---

    /** Um simulado com q1–q5 e as respostas na ordem (null = em branco). */
    private void prova(UUID aluno, boolean entregar, String... respostas) {
        var ids = q.stream().map(Questao::getId).toList();
        var simulado = simuladoRepository.save(new Simulado(aluno, "Prova", null, ids)).getId();
        sincronizar();
        var tentativa = execucao.iniciar(simulado, aluno).tentativa().getId();
        for (int i = 0; i < respostas.length; i++) {
            if (respostas[i] != null) {
                execucao.responder(tentativa, aluno, q.get(i).getId(), alternativa(q.get(i), respostas[i]));
            }
        }
        if (entregar) execucao.finalizar(tentativa, aluno);
        sincronizar();
    }

    private void executar(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
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

    private static Questao questao(int numero, Assunto assunto) {
        var questao = new Questao();
        questao.setAno(2021);
        questao.setNumero(numero + 135);
        questao.setArea(assunto.getArea());
        questao.setAssunto(assunto);
        questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
        questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2021/" + (numero + 135));
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
