package br.com.simulados.api.simulado.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import br.com.simulados.api.simulado.domain.Placar;
import br.com.simulados.api.simulado.domain.ResultadoIndisponivelException;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.TentativaNaoEncontradaException;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Banco real: o GROUP BY com LEFT JOIN nas respostas é o que se testa. Prova de 5:
 * probabilidade (q1 certa, q2 errada, q3 em branco) e ecologia (q4 e q5 certas).
 * Gabarito de toda questão: A. Esperado: 3 de 5.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ConsultaDeResultadoTest {

    @Autowired
    private ConsultaDeResultado consulta;

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
    private UUID simulado;
    private final List<Questao> q = new ArrayList<>();

    @BeforeEach
    void setUp() {
        aluna = usuario("aluna@exemplo.com");
        Map<String, Assunto> assuntos = assuntoRepository.findAll().stream()
                .collect(Collectors.toMap(Assunto::getSlug, a -> a));
        for (int n = 1; n <= 3; n++) q.add(questaoRepository.save(questao(n, assuntos.get("probabilidade"))));
        for (int n = 4; n <= 5; n++) q.add(questaoRepository.save(questao(n, assuntos.get("ecologia"))));
        simulado = simuladoRepository.save(new Simulado(aluna, "Treino", null, q.stream().map(Questao::getId).toList())).getId();
        sincronizar();
    }

    @Test
    void placarComEmBrancoPorAreaEPorAssuntoDoPiorProMelhor() {
        var tentativa = fazerAProva();

        var resultado = consulta.detalhar(tentativa, aluna);

        assertThat(resultado.total()).isEqualTo(new Placar(5, 4, 3));
        assertThat(resultado.total().percentual()).isEqualTo(60);
        assertThat(resultado.total().emBranco()).isEqualTo(1);
        // Na ordem fixa do ENEM: Natureza antes de Matemática
        assertThat(resultado.porArea()).containsExactly(
                Map.entry(Area.CIENCIAS_NATUREZA, new Placar(2, 2, 2)),
                Map.entry(Area.MATEMATICA, new Placar(3, 2, 1)));
        // Probabilidade (1 de 3) é pior que ecologia (2 de 2), e vem antes
        assertThat(resultado.porAssunto()).extracting(LinhaDoResultado::assuntoSlug, LinhaDoResultado::placar)
                .containsExactly(
                        tuple("probabilidade", new Placar(3, 2, 1)),
                        tuple("ecologia", new Placar(2, 2, 2)));
        assertThat(resultado.questoes()).extracting(Questao::getId)
                .containsExactlyElementsOf(q.stream().map(Questao::getId).toList());
        assertThat(resultado.marcadas()).hasSize(4);
    }

    @Test
    void provaEmAndamentoNaoTemResultado() {
        var tentativa = execucao.iniciar(simulado, aluna).tentativa().getId();

        assertThatThrownBy(() -> consulta.detalhar(tentativa, aluna))
                .isInstanceOf(ResultadoIndisponivelException.class);
    }

    @Test
    void resultadoAlheioEhComoSeNaoExistisse() {
        var tentativa = fazerAProva();

        assertThatThrownBy(() -> consulta.detalhar(tentativa, usuario("outra@exemplo.com")))
                .isInstanceOf(TentativaNaoEncontradaException.class);
    }

    @Test
    void questaoAnuladaDepoisDaProvaSaiDaNotaEDaRevisao() {
        var tentativa = fazerAProva();
        // q4 (ecologia, respondida certa) é anulada pelo INEP depois da entrega
        entityManager.createNativeQuery("UPDATE questoes SET anulada = true, gabarito = NULL WHERE id = :id")
                .setParameter("id", q.get(3).getId())
                .executeUpdate();
        sincronizar();

        var resultado = consulta.detalhar(tentativa, aluna);

        assertThat(resultado.total()).isEqualTo(new Placar(4, 3, 2));
        assertThat(resultado.porArea().get(Area.CIENCIAS_NATUREZA)).isEqualTo(new Placar(1, 1, 1));
        assertThat(resultado.questoes()).extracting(Questao::getId).doesNotContain(q.get(3).getId());
    }

    @Test
    void questaoSemAssuntoAindaContaNaNota() {
        // Questão não classificada: o LEFT JOIN no assunto não pode tirá-la da nota
        var semAssunto = questaoRepository.save(questao(6, null));
        var outro = simuladoRepository.save(new Simulado(aluna, "Sem assunto", null, List.of(semAssunto.getId()))).getId();
        sincronizar();
        var tentativa = execucao.iniciar(outro, aluna).tentativa().getId();
        execucao.responder(tentativa, aluna, semAssunto.getId(), alternativa(semAssunto, "A"));
        execucao.finalizar(tentativa, aluna);
        sincronizar();

        var resultado = consulta.detalhar(tentativa, aluna);

        assertThat(resultado.total()).isEqualTo(new Placar(1, 1, 1));
        assertThat(resultado.porAssunto()).singleElement()
                .satisfies(linha -> assertThat(linha.assuntoSlug()).isNull());
    }

    @Test
    void custaCincoQueriesFixas() {
        var tentativa = fazerAProva();
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        var resultado = consulta.detalhar(tentativa, aluna);
        // Tocar no assunto de cada questão não pode disparar SELECT (N+1 no getter)
        resultado.questoes().forEach(questao -> questao.getAssunto().getMateria().getNome());

        // tentativa + simulado + GROUP BY + questões (com assunto) + respostas
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(5);
    }

    // --- apoio ---

    /** q1 certa, q2 errada, q3 em branco, q4 e q5 certas; entregue. */
    private UUID fazerAProva() {
        var tentativa = execucao.iniciar(simulado, aluna).tentativa().getId();
        execucao.responder(tentativa, aluna, q.get(0).getId(), alternativa(q.get(0), "A"));
        execucao.responder(tentativa, aluna, q.get(1).getId(), alternativa(q.get(1), "C"));
        execucao.responder(tentativa, aluna, q.get(3).getId(), alternativa(q.get(3), "A"));
        execucao.responder(tentativa, aluna, q.get(4).getId(), alternativa(q.get(4), "A"));
        execucao.finalizar(tentativa, aluna);
        sincronizar();
        return tentativa;
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

    // Área vem do assunto (a FK composta da V4 exige a mesma); sem assunto, Matemática
    private static Questao questao(int numero, Assunto assunto) {
        var questao = new Questao();
        questao.setAno(2021);
        questao.setNumero(numero + 135);
        questao.setArea(assunto == null ? Area.MATEMATICA : assunto.getArea());
        if (assunto != null) {
            questao.setAssunto(assunto);
            questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
            questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        }
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
