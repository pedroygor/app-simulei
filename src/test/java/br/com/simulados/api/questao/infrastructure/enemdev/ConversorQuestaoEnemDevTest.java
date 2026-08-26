package br.com.simulados.api.questao.infrastructure.enemdev;

import br.com.simulados.api.questao.application.QuestaoInvalidaException;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unitário puro: nenhum I/O. A reescrita de imagem é injetada como função,
// então dá pra testar sem baixar nada.
class ConversorQuestaoEnemDevTest {

    private static final UnaryOperator<String> SEM_REESCRITA = UnaryOperator.identity();
    private static final UnaryOperator<String> PARA_LOCAL = url -> "/imagens/local/" + url.substring(url.lastIndexOf('/') + 1);

    private final ConversorQuestaoEnemDev conversor = new ConversorQuestaoEnemDev();

    @Test
    void converteQuestaoCompleta() {
        var questao = conversor.converter(questaoDaFonte(2023, 1, "linguagens", "espanhol"), SEM_REESCRITA);

        assertThat(questao.getAno()).isEqualTo(2023);
        assertThat(questao.getNumero()).isEqualTo(1);
        assertThat(questao.getArea()).isEqualTo(Area.LINGUAGENS);
        assertThat(questao.getIdioma()).isEqualTo(Idioma.ESPANHOL);
        assertThat(questao.getGabarito()).isEqualTo("A");
        assertThat(questao.isAnulada()).isFalse();
        assertThat(questao.getFonte()).isEqualTo("enem.dev");
        assertThat(questao.getFonteId()).isEqualTo("2023/1/espanhol");
        assertThat(questao.getAlternativas()).hasSize(5);
        // O relacionamento bidirecional tem que ficar coerente, senão o
        // questao_id vai nulo no INSERT
        assertThat(questao.getAlternativas().getFirst().getQuestao()).isSameAs(questao);
    }

    @Test
    void mapeiaAsQuatroAreasDaFonte() {
        assertThat(conversor.converter(questaoDaFonte(2023, 10, "linguagens", null), SEM_REESCRITA).getArea())
                .isEqualTo(Area.LINGUAGENS);
        assertThat(conversor.converter(questaoDaFonte(2023, 11, "ciencias-humanas", null), SEM_REESCRITA).getArea())
                .isEqualTo(Area.CIENCIAS_HUMANAS);
        assertThat(conversor.converter(questaoDaFonte(2023, 12, "ciencias-natureza", null), SEM_REESCRITA).getArea())
                .isEqualTo(Area.CIENCIAS_NATUREZA);
        assertThat(conversor.converter(questaoDaFonte(2023, 13, "matematica", null), SEM_REESCRITA).getArea())
                .isEqualTo(Area.MATEMATICA);
    }

    @Test
    void questaoSemIdiomaFicaComIdiomaNulo() {
        var questao = conversor.converter(questaoDaFonte(2023, 100, "matematica", null), SEM_REESCRITA);

        assertThat(questao.getIdioma()).isNull();
        assertThat(questao.getFonteId()).isEqualTo("2023/100");
    }

    @Test
    void reescreveUrlsDeImagemNoMarkdownDoEnunciado() {
        var origem = new QuestaoEnemDev(170, 2023, "matematica", null,
                "Veja ![grafico](https://enem.dev/2023/questions/170/abc.png) e também ![](https://enem.dev/2023/questions/170/def.png).",
                "Qual o valor?", "C",
                List.of("https://enem.dev/2023/questions/170/abc.png"),
                alternativasValidas());

        var questao = conversor.converter(origem, PARA_LOCAL);

        assertThat(questao.getEnunciado())
                .contains("/imagens/local/abc.png")
                .contains("/imagens/local/def.png")
                // A independência da fonte só vale se NENHUMA URL do enem.dev sobrar
                .doesNotContain("enem.dev");
        // O texto ao redor precisa sobreviver intacto
        assertThat(questao.getEnunciado()).startsWith("Veja ![grafico](").endsWith(").");
    }

    @Test
    void reescreveImagemDeAlternativa() {
        var alternativas = List.of(
                new AlternativaEnemDev("A", null, "https://enem.dev/2023/questions/1/alt-a.png", false),
                new AlternativaEnemDev("B", "texto b", null, true),
                new AlternativaEnemDev("C", "texto c", null, false),
                new AlternativaEnemDev("D", "texto d", null, false),
                new AlternativaEnemDev("E", "texto e", null, false));
        var origem = new QuestaoEnemDev(1, 2023, "matematica", null, "enunciado", "comando", "B", List.of(), alternativas);

        var questao = conversor.converter(origem, PARA_LOCAL);

        assertThat(questao.getAlternativas().getFirst().getImagemUrl()).isEqualTo("/imagens/local/alt-a.png");
        assertThat(questao.getAlternativas().getFirst().getTexto()).isNull();
        assertThat(questao.getAlternativas().get(1).getImagemUrl()).isNull();
    }

    @Test
    void aceitaQuestaoSemEnunciadoQuandoTemComando() {
        // 144 questões reais da fonte são assim (ex: 2009 q55): o texto-base
        // não existe e a pergunta inteira está no comando
        var origem = new QuestaoEnemDev(55, 2009, "ciencias-humanas", null,
                null, "O fim da Guerra Fria gerou expectativas de que...", "A", List.of(), alternativasValidas());

        var questao = conversor.converter(origem, SEM_REESCRITA);

        assertThat(questao.getEnunciado()).isNull();
        assertThat(questao.getComando()).isNotBlank();
    }

    @Test
    void rejeitaQuestaoComAlternativaEmBranco() {
        // Caso real: 2023 q132, alternativas A-D sem texto e sem imagem
        var alternativas = List.of(
                new AlternativaEnemDev("A", null, null, false),
                new AlternativaEnemDev("B", "texto", null, false),
                new AlternativaEnemDev("C", "texto", null, false),
                new AlternativaEnemDev("D", "texto", null, false),
                new AlternativaEnemDev("E", "texto", null, true));
        var origem = new QuestaoEnemDev(132, 2023, "ciencias-natureza", null, "enunciado", "comando", "E", List.of(), alternativas);

        assertThatThrownBy(() -> conversor.converter(origem, SEM_REESCRITA))
                .isInstanceOf(QuestaoInvalidaException.class)
                .hasMessageContaining("sem texto e sem imagem");
    }

    @Test
    void rejeitaQuestaoComMenosDeCincoAlternativas() {
        // 7 questões reais da fonte vêm com 4 (ex: 2011 q180)
        var origem = new QuestaoEnemDev(180, 2011, "matematica", null, "enunciado", "comando", "A",
                List.of(), alternativasValidas().subList(0, 4));

        assertThatThrownBy(() -> conversor.converter(origem, SEM_REESCRITA))
                .isInstanceOf(QuestaoInvalidaException.class)
                .hasMessageContaining("esperava 5 alternativas, veio 4");
    }

    @Test
    void rejeitaQuestaoSemEnunciadoESemComando() {
        var origem = new QuestaoEnemDev(1, 2023, "matematica", null, null, null, "A", List.of(), alternativasValidas());

        assertThatThrownBy(() -> conversor.converter(origem, SEM_REESCRITA))
                .isInstanceOf(QuestaoInvalidaException.class)
                .hasMessageContaining("sem enunciado e sem comando");
    }

    @Test
    void rejeitaAreaDesconhecida() {
        var origem = new QuestaoEnemDev(1, 2023, "disciplina-nova", null, "enunciado", "comando", "A",
                List.of(), alternativasValidas());

        assertThatThrownBy(() -> conversor.converter(origem, SEM_REESCRITA))
                .isInstanceOf(QuestaoInvalidaException.class)
                .hasMessageContaining("área desconhecida");
    }

    @Test
    void marcaComoAnuladaQuandoNaoHaGabarito() {
        var origem = new QuestaoEnemDev(1, 2023, "matematica", null, "enunciado", "comando", null,
                List.of(), alternativasValidas());

        var questao = conversor.converter(origem, SEM_REESCRITA);

        assertThat(questao.isAnulada()).isTrue();
        assertThat(questao.getGabarito()).isNull();
    }

    private QuestaoEnemDev questaoDaFonte(int ano, int numero, String discipline, String language) {
        return new QuestaoEnemDev(numero, ano, discipline, language,
                "Texto-base da questão", "Assinale a alternativa correta.", "A",
                List.of(), alternativasValidas());
    }

    private List<AlternativaEnemDev> alternativasValidas() {
        return List.of(
                new AlternativaEnemDev("A", "Alternativa A", null, true),
                new AlternativaEnemDev("B", "Alternativa B", null, false),
                new AlternativaEnemDev("C", "Alternativa C", null, false),
                new AlternativaEnemDev("D", "Alternativa D", null, false),
                new AlternativaEnemDev("E", "Alternativa E", null, false));
    }
}
