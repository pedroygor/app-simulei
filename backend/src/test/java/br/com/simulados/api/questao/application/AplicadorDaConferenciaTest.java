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

import static br.com.simulados.api.questao.application.TipoDeCorrecao.ANULADA;
import static br.com.simulados.api.questao.application.TipoDeCorrecao.GABARITO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real (Testcontainers): o que interessa é o que chega ao banco — o CHECK
// "anulada OR gabarito NOT NULL", o dirty checking, o número de queries. Com mock
// nada disso seria testado. @Transactional reverte cada teste; por isso os
// testes chamam flush() pra forçar os UPDATEs antes de conferir.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AplicadorDaConferenciaTest {

    @Autowired
    private AplicadorDaConferencia aplicador;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private EntityManager entityManager;

    private Long q15;
    private Long q163;
    private Long ingles1;

    @BeforeEach
    void setUp() {
        // Gabaritos como a enem.dev entregou: q15 errado (E, o INEP diz D) e q163
        // com letra, mas anulada pelo INEP
        q15 = questaoRepository.save(questao(2021, 15, null, "E", Area.LINGUAGENS)).getId();
        q163 = questaoRepository.save(questao(2018, 163, null, "A", Area.MATEMATICA)).getId();
        ingles1 = questaoRepository.save(questao(2021, 1, Idioma.INGLES, "C", Area.LINGUAGENS)).getId();
        // Mesmo número, outro idioma: não pode ser tocada por uma linha de INGLES
        questaoRepository.save(questao(2021, 1, Idioma.ESPANHOL, "C", Area.LINGUAGENS));
        sincronizar();
    }

    @Test
    void corrigeOGabaritoQuandoOBancoAindaTemOValorAntigo() {
        var resultado = aplicador.aplicar(List.of(gabarito(2, 2021, 15, null, "E", "D")));
        sincronizar();

        assertThat(resultado.gabaritosCorrigidos()).isEqualTo(1);
        assertThat(questaoRepository.findById(q15).orElseThrow().getGabarito()).isEqualTo("D");
    }

    @Test
    void idiomaFazParteDaChave() {
        aplicador.aplicar(List.of(gabarito(2, 2021, 1, Idioma.INGLES, "C", "B")));
        sincronizar();

        assertThat(questaoRepository.findById(ingles1).orElseThrow().getGabarito()).isEqualTo("B");
        // a variante em espanhol ficou como estava
        var espanhol = entityManager.createQuery(
                "select q from Questao q where q.ano = 2021 and q.numero = 1 and q.idioma = :i", Questao.class)
                .setParameter("i", Idioma.ESPANHOL).getSingleResult();
        assertThat(espanhol.getGabarito()).isEqualTo("C");
    }

    @Test
    void segundaRodadaComOMesmoArquivoNaoAlteraNada() {
        var linhas = List.of(gabarito(2, 2021, 15, null, "E", "D"), anulada(3, 2018, 163, "A"));
        aplicador.aplicar(linhas);
        sincronizar();

        var segunda = aplicador.aplicar(linhas);

        assertThat(segunda.gabaritosCorrigidos()).isZero();
        assertThat(segunda.questoesAnuladas()).isZero();
        assertThat(segunda.inalteradas()).isEqualTo(2);
    }

    @Test
    void naoSobrescreveUmGabaritoQueAlguemMudouDepois() {
        // O arquivo viu 'E' e propõe 'D'. Mas alguém corrigiu à mão pra 'B': nem o
        // valor antigo nem o do INEP. Não se atropela; vai pra decisão humana
        questaoRepository.findById(q15).orElseThrow().setGabarito("B");
        sincronizar();

        var resultado = aplicador.aplicar(List.of(gabarito(2, 2021, 15, null, "E", "D")));
        sincronizar();

        assertThat(resultado.gabaritosCorrigidos()).isZero();
        assertThat(resultado.divergentes()).hasSize(1).first().asString()
                .contains("linha 2").contains("2021/15").contains("'B'");
        assertThat(questaoRepository.findById(q15).orElseThrow().getGabarito()).isEqualTo("B");
    }

    @Test
    void anulaAQuestaoEZeraOGabarito() {
        var resultado = aplicador.aplicar(List.of(anulada(2, 2018, 163, "A")));
        sincronizar();

        assertThat(resultado.questoesAnuladas()).isEqualTo(1);
        var questao = questaoRepository.findById(q163).orElseThrow();
        assertThat(questao.isAnulada()).isTrue();
        // O CHECK da V2 (anulada OR gabarito NOT NULL) permite; anulada não tem gabarito
        assertThat(questao.getGabarito()).isNull();
    }

    @Test
    void questaoAnuladaDeixaDeAparecerNaApi() {
        assertThat(questaoRepository.buscarComAlternativas(q163)).isPresent();

        aplicador.aplicar(List.of(anulada(2, 2018, 163, "A")));
        sincronizar();

        // É o mesmo filtro (anulada = false) que a busca, a contagem por assunto e,
        // na 3a, o sorteio dos simulados usam
        assertThat(questaoRepository.buscarComAlternativas(q163)).isEmpty();
    }

    @Test
    void anulacaoNaoDependeDoGabaritoQueOBancoTinha() {
        // valor_banco é só o histórico do que a enem.dev entregou: o INEP anulou
        // a questão, e isso vale mesmo que alguém tenha mexido na letra
        questaoRepository.findById(q163).orElseThrow().setGabarito("C");
        sincronizar();

        var resultado = aplicador.aplicar(List.of(anulada(2, 2018, 163, "A")));

        assertThat(resultado.questoesAnuladas()).isEqualTo(1);
    }

    @Test
    void umaLinhaInvalidaRecusaOArquivoInteiroSemGravarNada() {
        var linhas = List.of(
                gabarito(2, 2021, 15, null, "E", "D"),
                gabarito(3, 2018, 163, null, "A", "Z"));

        assertThatThrownBy(() -> aplicador.aplicar(linhas))
                .isInstanceOf(ConferenciaInvalidaException.class)
                .hasMessageContaining("linha 3")
                .hasMessageContaining("'Z'");
        sincronizar();

        // A linha 2 era válida, mas não entrou: tudo ou nada
        assertThat(questaoRepository.findById(q15).orElseThrow().getGabarito()).isEqualTo("E");
    }

    @Test
    void anulacaoPrecisaDeXComoValorDoInep() {
        // Protege contra uma linha de GABARITO mal rotulada anular questão
        var linha = new LinhaDeConferencia(2, ANULADA, 2018, 163, null, "A", "D");

        assertThatThrownBy(() -> aplicador.aplicar(List.of(linha)))
                .isInstanceOf(ConferenciaInvalidaException.class)
                .hasMessageContaining("X");
    }

    @Test
    void recusaQuestaoRepetidaNoArquivo() {
        var linhas = List.of(gabarito(2, 2021, 15, null, "E", "D"), anulada(3, 2021, 15, "E"));

        assertThatThrownBy(() -> aplicador.aplicar(linhas))
                .isInstanceOf(ConferenciaInvalidaException.class)
                .hasMessageContaining("linha 3")
                .hasMessageContaining("mais de uma vez");
    }

    @Test
    void linhaSemQuestaoNoBancoEhContadaENaoRecusada() {
        // Banco de dev que ainda não importou 2009: não é erro do arquivo
        var resultado = aplicador.aplicar(List.of(
                gabarito(2, 2021, 15, null, "E", "D"),
                gabarito(3, 2009, 120, null, "D", "C")));

        assertThat(resultado.gabaritosCorrigidos()).isEqualTo(1);
        assertThat(resultado.semQuestao()).containsExactly("2009/120");
    }

    @Test
    void numeroDeQueriesNaoDependeDoTamanhoDoArquivo() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        aplicador.aplicar(List.of(
                gabarito(2, 2021, 15, null, "E", "D"),
                anulada(3, 2018, 163, "A"),
                gabarito(4, 2021, 1, Idioma.INGLES, "C", "B")));
        entityManager.flush();

        // 1 SELECT das questões + 1 lote de UPDATEs (batch_size). Uma busca por
        // linha seriam 3 SELECTs — e 20 na carga real
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(statistics.getEntityUpdateCount()).isEqualTo(3);
    }

    // --- apoio ---

    // flush grava o pendente; clear esvazia a sessão, pra a próxima leitura ir DE VERDADE ao banco
    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private static LinhaDeConferencia gabarito(int linha, int ano, int numero, Idioma idioma, String antes, String depois) {
        return new LinhaDeConferencia(linha, GABARITO, ano, numero, idioma, antes, depois);
    }

    private static LinhaDeConferencia anulada(int linha, int ano, int numero, String gabaritoQueOBancoTinha) {
        return new LinhaDeConferencia(linha, ANULADA, ano, numero, null, gabaritoQueOBancoTinha, "X");
    }

    private static Questao questao(int ano, int numero, Idioma idioma, String gabarito, Area area) {
        var questao = new Questao();
        questao.setAno(ano);
        questao.setNumero(numero);
        questao.setIdioma(idioma);
        questao.setArea(area);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito(gabarito);
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
