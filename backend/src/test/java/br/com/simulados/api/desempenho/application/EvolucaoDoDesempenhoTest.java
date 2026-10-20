package br.com.simulados.api.desempenho.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.desempenho.domain.Aproveitamento;
import br.com.simulados.api.desempenho.domain.SemanaDeDesempenho;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.AssuntoNaoEncontradoException;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.application.ExecucaoDaProva;
import br.com.simulados.api.simulado.domain.Simulado;
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

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static br.com.simulados.api.desempenho.application.EvolucaoDoDesempenho.FUSO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Banco real: date_trunc com AT TIME ZONE é o que se testa. O relógio é o de
 * verdade, então as datas das provas são fixadas por UPDATE em relação à semana
 * corrente (`estaSemana` = a segunda-feira de hoje, em Brasília).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EvolucaoDoDesempenhoTest {

    @Autowired
    private EvolucaoDoDesempenho evolucao;

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

    @Autowired
    private Clock clock;

    private UUID aluna;
    private LocalDate estaSemana;
    private final List<Questao> mat = new ArrayList<>();
    private Questao natureza;

    @BeforeEach
    void setUp() {
        aluna = usuario("aluna@exemplo.com");
        estaSemana = LocalDate.now(clock.withZone(FUSO)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<String, Assunto> assuntos = assuntoRepository.findAll().stream()
                .collect(Collectors.toMap(Assunto::getSlug, a -> a));
        for (int n = 1; n <= 4; n++) mat.add(questaoRepository.save(questao(n, assuntos.get("probabilidade"))));
        natureza = questaoRepository.save(questao(5, assuntos.get("ecologia")));
        sincronizar();
    }

    @Test
    void devolveTodaSemanaDaJanelaInclusiveAsVaziasEmOrdem() {
        var semanas = evolucao.doAluno(aluna, 4);

        assertThat(semanas).extracting(SemanaDeDesempenho::inicio).containsExactly(
                estaSemana.minusWeeks(3), estaSemana.minusWeeks(2), estaSemana.minusWeeks(1), estaSemana);
        // Semana vazia: percentual null (buraco), nunca 0%
        assertThat(semanas).allSatisfy(s -> {
            assertThat(s.geral().percentual()).isNull();
            assertThat(s.porArea()).isEmpty();
        });
    }

    @Test
    void cadaProvaContaNaSemanaEmQueFoiEntregueComGeralSomandoAsAreas() {
        // Semana passada: 2 de Matemática certas, 1 errada, 1 em branco; Natureza certa
        entregar(estaSemana.minusWeeks(1).plusDays(2), "A", "A", "B", null, "A");
        // Esta semana: 1 de 4 em Matemática
        entregar(estaSemana, "A", "B", "B", "B", null);

        var semanas = evolucao.doAluno(aluna, 2);

        var passada = semanas.get(0);
        assertThat(passada.porArea()).containsExactly(
                Map.entry(Area.CIENCIAS_NATUREZA, new Aproveitamento(1, 1, 0)),
                Map.entry(Area.MATEMATICA, new Aproveitamento(3, 2, 1)));
        assertThat(passada.geral()).isEqualTo(new Aproveitamento(4, 3, 1));
        assertThat(semanas.get(1).geral()).isEqualTo(new Aproveitamento(4, 1, 1));
    }

    @Test
    void domingoAsVinteEDuasEmBrasiliaEhDaSemanaPassadaMesmoJaSendoSegundaEmUtc() {
        // Domingo 22h em Brasília = segunda 01h em UTC: sem o AT TIME ZONE, cairia nesta semana
        var domingoANoite = estaSemana.minusDays(1).atTime(LocalTime.of(22, 0)).atZone(FUSO).toInstant();
        entregar(domingoANoite, "A", "A", "A", "A", "A");

        var semanas = evolucao.doAluno(aluna, 2);

        assertThat(semanas.get(0).geral().respondidas()).isEqualTo(5);
        assertThat(semanas.get(1).geral().respondidas()).isZero();
    }

    @Test
    void foraDaJanelaEmAndamentoEDeOutroAlunoNaoEntram() {
        entregar(estaSemana.minusWeeks(4), "A", "A", "A", "A", "A");   // janela de 4 começa em −3
        prova(aluna, false, "A", "A", "A", "A", "A");                     // em andamento
        var outra = usuario("outra@exemplo.com");
        prova(outra, true, "A", "A", "A", "A", "A");

        assertThat(evolucao.doAluno(aluna, 4)).allSatisfy(s -> assertThat(s.geral().respondidas()).isZero());
    }

    @Test
    void umaQuerySoQualquerQueSejaAJanela() {
        entregar(estaSemana, "A", "A", "A", "A", "A");
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        evolucao.doAluno(aluna, 52);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void evolucaoDeUmAssuntoContaSoAsQuestoesDele() {
        // Probabilidade: 2 certas, 1 errada, 1 em branco; a de ecologia não pode entrar
        entregar(estaSemana, "A", "A", "B", null, "B");

        var noAssunto = evolucao.doAlunoNoAssunto(aluna, 2, "probabilidade");

        assertThat(noAssunto.assunto().getNome()).isEqualTo("Probabilidade");
        assertThat(noAssunto.assunto().getMateria().getNome()).isEqualTo("Matemática");
        assertThat(noAssunto.semanas().get(1).geral()).isEqualTo(new Aproveitamento(3, 2, 1));
        assertThat(noAssunto.semanas().get(1).porArea()).containsOnlyKeys(Area.MATEMATICA);
    }

    @Test
    void assuntoInexistenteEhNaoEncontradoNaoEvolucaoVazia() {
        assertThatThrownBy(() -> evolucao.doAlunoNoAssunto(aluna, 4, "assunto-que-nao-existe"))
                .isInstanceOf(AssuntoNaoEncontradoException.class);
    }

    @Test
    void evolucaoDeUmAssuntoCustaDuasQueries() {
        entregar(estaSemana, "A", "A", "A", "A", "A");
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        evolucao.doAlunoNoAssunto(aluna, 12, "probabilidade");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    // --- apoio ---

    private void entregar(LocalDate dia, String... respostas) {
        entregar(dia.atTime(LocalTime.NOON).atZone(FUSO).toInstant(), respostas);
    }

    private void entregar(Instant quando, String... respostas) {
        var tentativa = prova(aluna, true, respostas);
        entityManager.createNativeQuery("UPDATE tentativas SET finalizado_em = :quando WHERE id = :id")
                .setParameter("quando", quando)
                .setParameter("id", tentativa)
                .executeUpdate();
        sincronizar();
    }

    /** As 5 questões (4 de Matemática e 1 de Natureza), respostas na ordem; null = em branco. */
    private UUID prova(UUID aluno, boolean entregar, String... respostas) {
        var questoes = new ArrayList<>(mat);
        questoes.add(natureza);
        var simulado = simuladoRepository.save(
                new Simulado(aluno, "Prova", null, questoes.stream().map(Questao::getId).toList())).getId();
        sincronizar();
        var tentativa = execucao.iniciar(simulado, aluno).tentativa().getId();
        for (int i = 0; i < respostas.length; i++) {
            if (respostas[i] != null) {
                execucao.responder(tentativa, aluno, questoes.get(i).getId(), alternativa(questoes.get(i), respostas[i]));
            }
        }
        if (entregar) execucao.finalizar(tentativa, aluno);
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

    private static Questao questao(int numero, Assunto assunto) {
        var questao = new Questao();
        questao.setAno(2019);
        questao.setNumero(numero + 140);
        questao.setArea(assunto.getArea());
        questao.setAssunto(assunto);
        questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
        questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2019/" + (numero + 140));
        for (var letra : List.of("A", "B", "C", "D", "E")) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
