package br.com.simulados.api.questao.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real (Testcontainers): o que interessa é o que chega ao banco — a FK
// composta da V8 (habilidade da mesma área), o dirty checking, o número de
// queries. @Transactional reverte cada teste; flush() força os UPDATEs
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ImportadorDeHabilidadesTest {

    @Autowired
    private ImportadorDeHabilidades importador;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private EntityManager entityManager;

    private Long natureza;
    private Long ingles;
    private Long espanhol;

    @BeforeEach
    void setUp() {
        natureza = questaoRepository.save(questao(2020, 100, null, Area.CIENCIAS_NATUREZA)).getId();
        ingles = questaoRepository.save(questao(2021, 1, Idioma.INGLES, Area.LINGUAGENS)).getId();
        espanhol = questaoRepository.save(questao(2021, 1, Idioma.ESPANHOL, Area.LINGUAGENS)).getId();
        sincronizar();
    }

    @Test
    void gravaHabilidadeEDificuldade() {
        var resultado = importador.importar(List.of(linha(2, 2020, 100, null, Area.CIENCIAS_NATUREZA, 17, 0.53)));
        sincronizar();

        assertThat(resultado.atualizadas()).isEqualTo(1);
        var questao = questaoRepository.findById(natureza).orElseThrow();
        assertThat(questao.getHabilidade()).isEqualTo((short) 17);
        assertThat(questao.getDificuldade()).isEqualTo(0.53);
    }

    @Test
    void oDetalheTrazOTextoDaHabilidadeDaPropriaArea() {
        importador.importar(List.of(linha(2, 2020, 100, null, Area.CIENCIAS_NATUREZA, 17, 0.53)));
        sincronizar();

        var habilidade = questaoRepository.buscarComAlternativas(natureza).orElseThrow().getHabilidadeOficial();

        // A H17 de Natureza, não a de Matemática: a FK composta (area, habilidade)
        assertThat(habilidade.getDescricao()).startsWith("Relacionar informações apresentadas em diferentes formas");
        assertThat(habilidade.getCompetencia().getNumero()).isEqualTo((short) 5);
    }

    @Test
    void soDificuldadeDeixaAHabilidadeNula() {
        // 2009 fora de Matemática: o INEP publica o parâmetro B, não a habilidade
        importador.importar(List.of(linha(2, 2020, 100, null, Area.CIENCIAS_NATUREZA, null, -1.7)));
        sincronizar();

        var questao = questaoRepository.buscarComAlternativas(natureza).orElseThrow();
        assertThat(questao.getHabilidade()).isNull();
        assertThat(questao.getHabilidadeOficial()).isNull();
        assertThat(questao.getDificuldade()).isEqualTo(-1.7);
    }

    @Test
    void idiomaFazParteDaChave() {
        importador.importar(List.of(linha(2, 2021, 1, Idioma.INGLES, Area.LINGUAGENS, 6, 1.0)));
        sincronizar();

        assertThat(questaoRepository.findById(ingles).orElseThrow().getHabilidade()).isEqualTo((short) 6);
        assertThat(questaoRepository.findById(espanhol).orElseThrow().getHabilidade()).isNull();
    }

    @Test
    void segundaRodadaComOMesmoArquivoNaoAlteraNada() {
        var linhas = List.of(linha(2, 2020, 100, null, Area.CIENCIAS_NATUREZA, 17, 0.53));
        importador.importar(linhas);
        sincronizar();

        var segunda = importador.importar(linhas);

        assertThat(segunda.atualizadas()).isZero();
        assertThat(segunda.inalteradas()).isEqualTo(1);
    }

    @Test
    void areaDiferenteRecusaOArquivoInteiroSemGravarNada() {
        // A 2ª linha está certa, mas a 1ª diz que a questão de Natureza é de Matemática:
        // o casamento caiu no lugar errado, e nada pode ser gravado
        assertThatThrownBy(() -> importador.importar(List.of(
                linha(2, 2020, 100, null, Area.MATEMATICA, 17, 0.53),
                linha(3, 2021, 1, Idioma.INGLES, Area.LINGUAGENS, 6, 1.0))))
                .isInstanceOf(HabilidadesInvalidasException.class)
                .hasMessageContaining("linha 2");
        sincronizar();

        assertThat(questaoRepository.findById(ingles).orElseThrow().getHabilidade()).isNull();
    }

    @Test
    void recusaQuestaoRepetida() {
        assertThatThrownBy(() -> importador.importar(List.of(
                linha(2, 2020, 100, null, Area.CIENCIAS_NATUREZA, 17, 0.53),
                linha(3, 2020, 100, null, Area.CIENCIAS_NATUREZA, 18, 0.53))))
                .isInstanceOf(HabilidadesInvalidasException.class)
                .hasMessageContaining("mais de uma vez");
    }

    @Test
    void linhaSemQuestaoNoBancoEContadaNaoRecusada() {
        var resultado = importador.importar(List.of(linha(2, 2009, 1, null, Area.CIENCIAS_NATUREZA, null, -1.7)));

        assertThat(resultado.semQuestao()).containsExactly("2009/1");
    }

    @Test
    void numeroDeQueriesNaoDependeDoTamanhoDoArquivo() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        importador.importar(List.of(
                linha(2, 2020, 100, null, Area.CIENCIAS_NATUREZA, 17, 0.53),
                linha(3, 2021, 1, Idioma.INGLES, Area.LINGUAGENS, 6, 1.0),
                linha(4, 2021, 1, Idioma.ESPANHOL, Area.LINGUAGENS, 7, 1.1)));
        entityManager.flush();

        // 1 SELECT das questões + 1 lote de UPDATEs (batch_size)
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(statistics.getEntityUpdateCount()).isEqualTo(3);
    }

    // --- apoio ---

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private static LinhaDeHabilidade linha(int linha, int ano, int numero, Idioma idioma, Area area,
                                           Integer habilidade, Double dificuldade) {
        return new LinhaDeHabilidade(linha, ano, numero, idioma, area,
                habilidade == null ? null : habilidade.shortValue(), dificuldade);
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
