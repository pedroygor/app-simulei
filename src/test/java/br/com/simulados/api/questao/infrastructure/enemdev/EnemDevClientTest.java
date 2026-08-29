package br.com.simulados.api.questao.infrastructure.enemdev;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

// Unitário sem rede: o MockRestServiceServer se acopla ao builder do RestClient
// e intercepta as requests — testamos URL montada e desserialização de verdade
class EnemDevClientTest {

    private MockRestServiceServer servidor;
    private EnemDevClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://api.enem.dev/v1");
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new EnemDevClient(builder.build());
    }

    @Test
    void listaProvasDisponiveis() {
        servidor.expect(requestTo("https://api.enem.dev/v1/exams"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        [
                          {"title": "ENEM 2023", "year": 2023},
                          {"title": "ENEM 2022", "year": 2022}
                        ]
                        """, MediaType.APPLICATION_JSON));

        var provas = client.listarProvas();

        assertThat(provas).hasSize(2);
        assertThat(provas.getFirst().year()).isEqualTo(2023);
        servidor.verify();
    }

    @Test
    void buscaPaginaDeQuestoesComIdioma() {
        servidor.expect(requestTo(
                        "https://api.enem.dev/v1/exams/2023/questions?limit=10&offset=0&language=ingles"))
                .andExpect(method(GET))
                .andRespond(withSuccess(PAGINA_JSON, MediaType.APPLICATION_JSON));

        var pagina = client.buscarQuestoes(2023, 10, 0, "ingles");

        assertThat(pagina.metadata().total()).isEqualTo(183);
        assertThat(pagina.metadata().hasMore()).isTrue();

        var questao = pagina.questions().getFirst();
        assertThat(questao.index()).isEqualTo(1);
        assertThat(questao.discipline()).isEqualTo("linguagens");
        assertThat(questao.language()).isEqualTo("ingles");
        assertThat(questao.correctAlternative()).isEqualTo("B");
        assertThat(questao.files()).containsExactly("https://enem.dev/2023/questions/1/img.png");
        // Alternativa só-imagem: text null, file preenchido
        assertThat(questao.alternatives().getFirst().text()).isNull();
        assertThat(questao.alternatives().getFirst().file()).isNotNull();
        assertThat(questao.alternatives().get(1).isCorrect()).isTrue();
        servidor.verify();
    }

    @Test
    void omiteParametroDeIdiomaQuandoNulo() {
        servidor.expect(requestTo("https://api.enem.dev/v1/exams/2019/questions?limit=50&offset=100"))
                .andExpect(method(GET))
                .andExpect(queryParam("limit", "50"))
                .andExpect(queryParam("offset", "100"))
                .andRespond(withSuccess("""
                        {"metadata": {"limit": 50, "offset": 100, "total": 180, "hasMore": false},
                         "questions": []}
                        """, MediaType.APPLICATION_JSON));

        var pagina = client.buscarQuestoes(2019, 50, 100, null);

        assertThat(pagina.questions()).isEmpty();
        assertThat(pagina.metadata().hasMore()).isFalse();
        servidor.verify();
    }

    @Test
    void traduzErroHttpParaExcecaoPropria() {
        servidor.expect(requestTo("https://api.enem.dev/v1/exams"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.listarProvas())
                .isInstanceOf(EnemDevClientException.class)
                .hasMessageContaining("enem.dev");
    }

    private static final String PAGINA_JSON = """
            {
              "metadata": {"limit": 10, "offset": 0, "total": 183, "hasMore": true},
              "questions": [
                {
                  "title": "Questão 1 - ENEM 2023",
                  "index": 1,
                  "year": 2023,
                  "discipline": "linguagens",
                  "language": "ingles",
                  "context": "Texto-base em **markdown** com ![](https://enem.dev/2023/questions/1/img.png)",
                  "alternativesIntroduction": "Assinale a alternativa correta.",
                  "correctAlternative": "B",
                  "files": ["https://enem.dev/2023/questions/1/img.png"],
                  "alternatives": [
                    {"letter": "A", "text": null, "file": "https://enem.dev/2023/questions/1/alt-a.png", "isCorrect": false},
                    {"letter": "B", "text": "Alternativa correta", "file": null, "isCorrect": true}
                  ]
                }
              ]
            }
            """;
}
