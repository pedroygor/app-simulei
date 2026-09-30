package br.com.simulados.api.questao.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.com.simulados.api.questao.domain.ConfiancaDaClassificacao.ALTA;
import static br.com.simulados.api.questao.domain.ConfiancaDaClassificacao.BAIXA;
import static br.com.simulados.api.questao.domain.ConfiancaDaClassificacao.MEDIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real (Testcontainers): o que interessa aqui é o que chega ao banco
// — FK composta, dirty checking, número de queries. Com mock nada disso
// seria testado. @Transactional reverte cada teste, e a transação do
// importador se junta à do teste; por isso os testes chamam flush() pra
// forçar os UPDATEs antes de conferir.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ImportadorDeClassificacaoTest {

    @Autowired
    private ImportadorDeClassificacao importador;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private EntityManager entityManager;

    private Long matematica136;
    private Long inglesQ1;

    @BeforeEach
    void setUp() {
        matematica136 = questaoRepository.save(questao(2023, 136, null, Area.MATEMATICA)).getId();
        questaoRepository.save(questao(2023, 137, null, Area.MATEMATICA));
        inglesQ1 = questaoRepository.save(questao(2023, 1, Idioma.INGLES, Area.LINGUAGENS)).getId();
        sincronizar();
    }

    @Test
    void gravaAssuntoConfiancaEOrigemIa() {
        var resultado = importador.importar(List.of(
                linha(2, 2023, 136, null, "probabilidade", ALTA),
                linha(3, 2023, 1, Idioma.INGLES, "interpretacao-ingles", MEDIA)));
        sincronizar();

        assertThat(resultado.atualizadas()).isEqualTo(2);
        var questao = questaoRepository.findById(matematica136).orElseThrow();
        assertThat(questao.getAssunto().getSlug()).isEqualTo("probabilidade");
        assertThat(questao.getClassificacaoConfianca()).isEqualTo(ALTA);
        assertThat(questao.getClassificacaoOrigem()).isEqualTo(OrigemDaClassificacao.IA);
        // Idioma faz parte da chave: a linha INGLES casou com a questão INGLES
        assertThat(questaoRepository.findById(inglesQ1).orElseThrow().getAssunto().getSlug())
                .isEqualTo("interpretacao-ingles");
    }

    @Test
    void segundaRodadaComOMesmoArquivoNaoAlteraNada() {
        var linhas = List.of(
                linha(2, 2023, 136, null, "probabilidade", ALTA),
                linha(3, 2023, 137, null, "estatistica", MEDIA));
        importador.importar(linhas);
        sincronizar();

        var segunda = importador.importar(linhas);

        assertThat(segunda.atualizadas()).isZero();
        assertThat(segunda.inalteradas()).isEqualTo(2);
    }

    @Test
    void mudarSoAConfiancaContaComoAtualizacao() {
        importador.importar(List.of(linha(2, 2023, 136, null, "probabilidade", ALTA)));
        sincronizar();

        var resultado = importador.importar(List.of(linha(2, 2023, 136, null, "probabilidade", BAIXA)));
        sincronizar();

        assertThat(resultado.atualizadas()).isEqualTo(1);
        assertThat(questaoRepository.findById(matematica136).orElseThrow().getClassificacaoConfianca())
                .isEqualTo(BAIXA);
    }

    @Test
    void naoSobrescreveRevisaoHumana() {
        var revisada = questaoRepository.findById(matematica136).orElseThrow();
        revisada.setAssunto(assunto("geometria-plana"));
        revisada.setClassificacaoOrigem(OrigemDaClassificacao.REVISAO_HUMANA);
        revisada.setClassificacaoConfianca(ALTA);
        sincronizar();

        var resultado = importador.importar(List.of(linha(2, 2023, 136, null, "probabilidade", ALTA)));
        sincronizar();

        assertThat(resultado.preservadasPorRevisaoHumana()).isEqualTo(1);
        assertThat(resultado.atualizadas()).isZero();
        var questao = questaoRepository.findById(matematica136).orElseThrow();
        assertThat(questao.getAssunto().getSlug()).isEqualTo("geometria-plana");
        assertThat(questao.getClassificacaoOrigem()).isEqualTo(OrigemDaClassificacao.REVISAO_HUMANA);
    }

    @Test
    void umaLinhaInvalidaRecusaACargaInteiraSemGravarNada() {
        var linhas = List.of(
                linha(2, 2023, 136, null, "probabilidade", ALTA),
                linha(3, 2023, 137, null, "assunto-que-nao-existe", ALTA));

        assertThatThrownBy(() -> importador.importar(linhas))
                .isInstanceOf(ClassificacaoInvalidaException.class)
                .hasMessageContaining("linha 3")
                .hasMessageContaining("assunto-que-nao-existe");
        sincronizar();

        // A linha 2 era válida, mas não entrou: tudo ou nada
        assertThat(questaoRepository.findById(matematica136).orElseThrow().getAssunto()).isNull();
    }

    @Test
    void recusaAssuntoDeOutraArea() {
        // genetica é de Ciências da Natureza; a questão é de Matemática
        assertThatThrownBy(() -> importador.importar(List.of(linha(2, 2023, 136, null, "genetica", ALTA))))
                .isInstanceOf(ClassificacaoInvalidaException.class)
                .hasMessageContaining("MATEMATICA")
                .hasMessageContaining("CIENCIAS_NATUREZA");
    }

    @Test
    void recusaQuestaoRepetidaNoArquivo() {
        var linhas = List.of(
                linha(2, 2023, 136, null, "probabilidade", ALTA),
                linha(3, 2023, 136, null, "estatistica", ALTA));

        assertThatThrownBy(() -> importador.importar(linhas))
                .isInstanceOf(ClassificacaoInvalidaException.class)
                .hasMessageContaining("linha 3")
                .hasMessageContaining("repetida");
    }

    @Test
    void juntaTodosOsProblemasNumaExcecaoSo() {
        var linhas = List.of(
                linha(2, 2023, 136, null, "nao-existe-1", ALTA),
                linha(3, 2023, 137, null, "nao-existe-2", ALTA));

        assertThatThrownBy(() -> importador.importar(linhas))
                .isInstanceOfSatisfying(ClassificacaoInvalidaException.class,
                        e -> assertThat(e.getProblemas()).hasSize(2));
    }

    @Test
    void linhaSemQuestaoNoBancoEhContadaENaoRecusada() {
        // Banco de dev que ainda não importou 2009: não é erro do arquivo
        var resultado = importador.importar(List.of(
                linha(2, 2023, 136, null, "probabilidade", ALTA),
                linha(3, 2009, 136, null, "probabilidade", ALTA)));

        assertThat(resultado.atualizadas()).isEqualTo(1);
        assertThat(resultado.semQuestao()).containsExactly("2009/136");
    }

    @Test
    void numeroDeQueriesNaoDependeDoTamanhoDoArquivo() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        importador.importar(List.of(
                linha(2, 2023, 136, null, "probabilidade", ALTA),
                linha(3, 2023, 137, null, "estatistica", MEDIA),
                linha(4, 2023, 1, Idioma.INGLES, "interpretacao-ingles", MEDIA)));
        entityManager.flush();

        // 1 SELECT dos assuntos + 1 SELECT das questões + 1 lote de UPDATEs
        // (batch_size). Sem o lote seriam 3 UPDATEs; com uma busca por linha,
        // mais 3 SELECTs — e 2.741 de cada na carga real
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        assertThat(statistics.getEntityUpdateCount()).isEqualTo(3);
    }

    // --- apoio ---

    // flush grava o pendente; clear esvazia a sessão, pra a próxima leitura
    // ir DE VERDADE ao banco
    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private Assunto assunto(String slug) {
        return entityManager.createQuery("SELECT a FROM Assunto a WHERE a.slug = :slug", Assunto.class)
                .setParameter("slug", slug)
                .getSingleResult();
    }

    private static LinhaDeClassificacao linha(int numeroDaLinha, int ano, int numero, Idioma idioma,
                                              String assunto, ConfiancaDaClassificacao confianca) {
        return new LinhaDeClassificacao(numeroDaLinha, ano, numero, idioma, assunto, confianca);
    }

    private static Questao questao(int ano, int numero, Idioma idioma, Area area) {
        var questao = new Questao();
        questao.setAno(ano);
        questao.setNumero(numero);
        questao.setIdioma(idioma);
        questao.setArea(area);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId(ano + "/" + numero + (idioma != null ? "/" + idioma : ""));
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
