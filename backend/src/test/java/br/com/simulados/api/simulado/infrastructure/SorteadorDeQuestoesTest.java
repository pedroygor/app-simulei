package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Banco real: janela, unnest de arrays e a coluna gerada `disponivel` só existem
// no Postgres. Questões numeradas por assunto pra o teste saber de onde cada
// sorteada veio: probabilidade 1xx, estatística 2xx, ecologia 3xx
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SorteadorDeQuestoesTest {

    @Autowired
    private SorteadorDeQuestoes sorteador;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private AssuntoRepository assuntoRepository;

    @Autowired
    private EntityManager entityManager;

    private Integer probabilidade;
    private Integer estatistica;
    private Integer ecologia;
    private UUID aluno;

    @BeforeEach
    void setUp() {
        var assuntos = assuntoRepository.findAll().stream().collect(Collectors.toMap(Assunto::getSlug, a -> a));
        probabilidade = assuntos.get("probabilidade").getId();
        estatistica = assuntos.get("estatistica").getId();
        ecologia = assuntos.get("ecologia").getId();

        // 6 de probabilidade: 4 em 2023, 2 em 2022; mais 1 anulada e 1 incompleta
        for (int n = 1; n <= 6; n++) {
            salvar(n <= 4 ? 2023 : 2022, 100 + n, assuntos.get("probabilidade"), false, false);
        }
        salvar(2023, 107, assuntos.get("probabilidade"), true, false);
        salvar(2023, 108, assuntos.get("probabilidade"), false, true);
        // 5 de estatística (2023) e 5 de ecologia (2023)
        for (int n = 1; n <= 5; n++) {
            salvar(2023, 200 + n, assuntos.get("estatistica"), false, false);
            salvar(2023, 300 + n, assuntos.get("ecologia"), false, false);
        }
        entityManager.flush();
        entityManager.clear();
        aluno = usuario("aluno@exemplo.com");
    }

    @Test
    void contaSoAsDisponiveisPorAssunto() {
        var contagem = sorteador.contarPorAssunto(Set.of(probabilidade, estatistica), List.of());

        // A anulada e a incompleta de probabilidade não contam
        assertThat(contagem).containsExactlyInAnyOrderEntriesOf(Map.of(probabilidade, 6L, estatistica, 5L));
    }

    @Test
    void contaSoNosAnosPedidos() {
        var contagem = sorteador.contarPorAssunto(Set.of(probabilidade), List.of(2022));

        assertThat(contagem).containsExactlyEntriesOf(Map.of(probabilidade, 2L));
    }

    @Test
    void sorteiaACotaDeCadaGrupoNaOrdemDasEscolhas() {
        // Grupo 0 = Natureza (ecologia), grupo 1 = Matemática (probabilidade + estatística)
        var ids = sorteador.sortear(aluno, Map.of(ecologia, 0, probabilidade, 1, estatistica, 1), List.of(2, 7), List.of());

        var numeros = numeros(ids);
        assertThat(numeros).hasSize(9).doesNotHaveDuplicates();
        assertThat(numeros.subList(0, 2)).allMatch(n -> n > 300);                  // Natureza primeiro
        assertThat(numeros.subList(2, 9)).allMatch(n -> n > 100 && n < 300);       // depois Matemática
    }

    @Test
    void nuncaSorteiaAnuladaNemIncompleta() {
        // Pede todas as 6 disponíveis de probabilidade, 20 vezes: 107 e 108 nunca saem
        for (int vez = 0; vez < 20; vez++) {
            var numeros = numeros(sorteador.sortear(aluno, Map.of(probabilidade, 0), List.of(6), List.of()));
            assertThat(numeros).containsExactlyInAnyOrder(101, 102, 103, 104, 105, 106);
        }
    }

    @Test
    void respeitaOsAnos() {
        var numeros = numeros(sorteador.sortear(aluno, Map.of(probabilidade, 0), List.of(2), List.of(2022)));

        assertThat(numeros).containsExactlyInAnyOrder(105, 106);
    }

    @Test
    void ordemEhAleatoria() {
        // Em 30 sorteios de 3 entre 6, sair sempre o mesmo trio seria sorteio falso
        var trios = new HashSet<List<Integer>>();
        for (int vez = 0; vez < 30; vez++) {
            trios.add(numeros(sorteador.sortear(aluno, Map.of(probabilidade, 0), List.of(3), List.of())));
        }
        assertThat(trios).hasSizeGreaterThan(1);
    }

    @Test
    void cadaConsultaEhUmaQuerySoQualquerQueSejaONumeroDeEscolhas() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        sorteador.contarPorAssunto(Set.of(probabilidade, estatistica, ecologia), List.of(2023));
        sorteador.sortear(aluno, Map.of(probabilidade, 0, estatistica, 1, ecologia, 2), List.of(2, 2, 2), List.of(2023));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    // --- apoio ---

    // --- inéditas primeiro (4b) ---

    @Test
    void sorteiaAsIneditasAntesDasJaVistas() {
        verEmProva(aluno, true, 101, 102, 103, 104);

        // Sobram 2 inéditas (105, 106) e a cota é 2: sempre as duas, nunca uma vista
        for (int i = 0; i < 10; i++) {
            assertThat(numeros(sorteador.sortear(aluno, Map.of(probabilidade, 0), List.of(2), List.of())))
                    .containsExactlyInAnyOrder(105, 106);
        }
    }

    @Test
    void quandoAsIneditasAcabamCompletaComAsJaVistas() {
        verEmProva(aluno, true, 101, 102, 103, 104);

        var numeros = numeros(sorteador.sortear(aluno, Map.of(probabilidade, 0), List.of(4), List.of()));

        // As inéditas vêm primeiro na ordem da prova; depois, 2 das já vistas
        assertThat(numeros.subList(0, 2)).containsExactlyInAnyOrder(105, 106);
        assertThat(numeros.subList(2, 4)).allMatch(n -> n >= 101 && n <= 104);
    }

    @Test
    void provaMontadaENaoComecadaOuDeOutroAlunoNaoContaComoVista() {
        verEmProva(aluno, false, 101, 102, 103, 104);
        verEmProva(usuario("outro@exemplo.com"), true, 101, 102, 103, 104);

        // Sem histórico de verdade, as 6 concorrem igual: em 20 sorteios de 2, alguma
        // de 101–104 aparece (a chance de nunca aparecer é (1/15)^20)
        var vistas = new HashSet<Integer>();
        for (int i = 0; i < 20; i++) {
            vistas.addAll(numeros(sorteador.sortear(aluno, Map.of(probabilidade, 0), List.of(2), List.of())));
        }
        assertThat(vistas).containsAnyOf(101, 102, 103, 104);
    }

    // --- apoio ---

    private UUID usuario(String email) {
        return (UUID) entityManager.createNativeQuery(
                        "INSERT INTO usuarios (id, nome, email, senha_hash) VALUES (gen_random_uuid(), 'Aluno', :email, 'hash') RETURNING id")
                .setParameter("email", email)
                .getSingleResult();
    }

    /** Um simulado do aluno com estas questões; `iniciada` = ele começou a prova (há tentativa). */
    private void verEmProva(UUID dono, boolean iniciada, int... numeros) {
        var simulado = (UUID) entityManager.createNativeQuery(
                        "INSERT INTO simulados (id, criado_por, titulo) VALUES (gen_random_uuid(), :dono, 'Prova') RETURNING id")
                .setParameter("dono", dono)
                .getSingleResult();
        for (int i = 0; i < numeros.length; i++) {
            entityManager.createNativeQuery("""
                            INSERT INTO simulado_questoes (simulado_id, questao_id, ordem)
                            SELECT :simulado, id, :ordem FROM questoes WHERE numero = :numero
                            """)
                    .setParameter("simulado", simulado)
                    .setParameter("ordem", i + 1)
                    .setParameter("numero", numeros[i])
                    .executeUpdate();
        }
        if (iniciada) {
            entityManager.createNativeQuery(
                            "INSERT INTO tentativas (simulado_id, aluno_id, status) VALUES (:simulado, :dono, 'EM_ANDAMENTO')")
                    .setParameter("simulado", simulado)
                    .setParameter("dono", dono)
                    .executeUpdate();
        }
    }

    // Os ids não dizem o assunto; o número da questão diz (1xx, 2xx, 3xx)
    private List<Integer> numeros(List<Long> ids) {
        var porId = questaoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Questao::getId, Questao::getNumero));
        return ids.stream().map(porId::get).toList();
    }

    private void salvar(int ano, int numero, Assunto assunto, boolean anulada, boolean incompleta) {
        var questao = new Questao();
        questao.setAno(ano);
        questao.setNumero(numero);
        questao.setArea(assunto.getArea());
        questao.setAssunto(assunto);
        questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
        questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setAnulada(anulada);
        questao.setGabarito(anulada ? null : "A");
        questao.setIncompleta(incompleta);
        questao.setFonte("enem.dev");
        questao.setFonteId(ano + "/" + numero);
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        questaoRepository.save(questao);
    }
}
