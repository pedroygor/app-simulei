package br.com.simulados.api.questao.api;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class QuestaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Value("${storage.imagens.diretorio}")
    private String diretorioDeImagens;

    @BeforeEach
    void semearQuestoes() {
        questaoRepository.deleteAll();
        questaoRepository.saveAll(java.util.List.of(
                questao(2023, 1, Area.LINGUAGENS, Idioma.ESPANHOL, false),
                questao(2023, 2, Area.LINGUAGENS, Idioma.INGLES, false),
                questao(2023, 90, Area.MATEMATICA, null, false),
                questao(2022, 91, Area.MATEMATICA, null, false),
                questao(2021, 92, Area.CIENCIAS_HUMANAS, null, false),
                // Anulada: não pode aparecer em lugar nenhum
                questao(2020, 93, Area.MATEMATICA, null, true)));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @WithMockUser
    void listaQuestoesPaginadasExcluindoAnuladas() throws Exception {
        mockMvc.perform(get("/questoes"))
                .andExpect(status().isOk())
                // 6 semeadas, 1 anulada -> 5
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.content").isArray())
                // Ordenação padrão: ano desc
                .andExpect(jsonPath("$.content[0].ano").value(2023))
                // Listagem não traz alternativas de propósito (ver N+1 abaixo)
                .andExpect(jsonPath("$.content[0].alternativas").doesNotExist())
                .andExpect(jsonPath("$.content[0].previa").isNotEmpty());
    }

    @Test
    @WithMockUser
    void listagemNaoFazNMaisUm() throws Exception {
        // Blindagem contra o erro clássico: se alguém colocar as alternativas
        // no DTO de listagem, cada questão da página vira um SELECT extra e
        // este teste quebra.
        //
        // size=2 força a paginação de verdade: com uma página que caberia
        // inteira, o Spring Data PULA a query de count (otimização do
        // PageableExecutionUtils) e o total cairia pra 1, escondendo o que
        // queremos medir.
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        mockMvc.perform(get("/questoes").param("size", "2")).andExpect(status().isOk());

        // 1 query de dados + 1 de count. Com N+1 seriam 2 + 2 (uma por questão
        // da página) e o custo cresceria junto com o tamanho da página.
        assertThat(statistics.getPrepareStatementCount())
                .as("listagem deve custar 2 queries (dados + count), não uma por questão")
                .isEqualTo(2);
    }

    @Test
    @WithMockUser
    void detalheCarregaAlternativasEmUmaQuerySo() throws Exception {
        var id = questaoRepository.findAll().stream()
                .filter(q -> !q.isAnulada()).findFirst().orElseThrow().getId();
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        mockMvc.perform(get("/questoes/{id}", id)).andExpect(status().isOk());

        // O JOIN FETCH traz questão + alternativas juntas. Sem ele seriam 2:
        // uma pra questão e outra quando o mapper tocasse na lista lazy.
        assertThat(statistics.getPrepareStatementCount())
                .as("detalhe deve custar 1 query graças ao JOIN FETCH")
                .isEqualTo(1);
    }

    @Test
    @WithMockUser
    void filtraPorArea() throws Exception {
        mockMvc.perform(get("/questoes").param("area", "MATEMATICA"))
                .andExpect(status().isOk())
                // 3 de matemática, mas 1 é anulada
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @WithMockUser
    void filtraPorIdioma() throws Exception {
        mockMvc.perform(get("/questoes").param("idioma", "INGLES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].idioma").value("INGLES"));
    }

    @Test
    @WithMockUser
    void filtraPorIntervaloDeAnos() throws Exception {
        mockMvc.perform(get("/questoes").param("anoInicial", "2022").param("anoFinal", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    @WithMockUser
    void toleraIntervaloDeAnosInvertido() throws Exception {
        // ?anoInicial=2023&anoFinal=2022 devolveria lista vazia se passássemos
        // os valores crus pro banco
        mockMvc.perform(get("/questoes").param("anoInicial", "2023").param("anoFinal", "2022"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    @WithMockUser
    void combinaFiltros() throws Exception {
        mockMvc.perform(get("/questoes")
                        .param("area", "LINGUAGENS")
                        .param("idioma", "ESPANHOL")
                        .param("anoInicial", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void respeitaTamanhoDePagina() throws Exception {
        mockMvc.perform(get("/questoes").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    @WithMockUser
    void rejeitaAnoForaDoIntervaloValido() throws Exception {
        mockMvc.perform(get("/questoes").param("anoInicial", "1500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    @WithMockUser
    void rejeitaAreaInexistenteSemVazarNomeDeClasseJava() throws Exception {
        mockMvc.perform(get("/questoes").param("area", "FILOSOFIA_QUANTICA"))
                .andExpect(status().isBadRequest())
                // A mensagem padrão do Spring citaria
                // "br.com.simulados.api.questao.domain.Area" — detalhe interno
                .andExpect(jsonPath("$.campos[0].mensagem").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("br.com.simulados"))))
                // ...e no lugar dela o frontend recebe os valores aceitos
                .andExpect(jsonPath("$.campos[0].mensagem").value(
                        org.hamcrest.Matchers.containsString("MATEMATICA")));
    }

    @Test
    @WithMockUser
    void ordenacaoPorCampoInexistenteDevolve400ENao500() throws Exception {
        mockMvc.perform(get("/questoes").param("sort", "campoQueNaoExiste"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("ordenação")));
    }

    @Test
    void listagemExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/questoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void detalheTrazAlternativasOrdenadas() throws Exception {
        var id = questaoRepository.findAll().stream()
                .filter(q -> !q.isAnulada()).findFirst().orElseThrow().getId();

        mockMvc.perform(get("/questoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enunciado").isNotEmpty())
                .andExpect(jsonPath("$.alternativas.length()").value(5))
                .andExpect(jsonPath("$.alternativas[0].letra").value("A"))
                .andExpect(jsonPath("$.alternativas[4].letra").value("E"));
    }

    @Test
    @WithMockUser
    void detalheDeQuestaoAnuladaDevolve404() throws Exception {
        var idAnulada = questaoRepository.findAll().stream()
                .filter(Questao::isAnulada).findFirst().orElseThrow().getId();

        mockMvc.perform(get("/questoes/{id}", idAnulada))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    void detalheDeIdInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/questoes/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void imagensSaoPublicas() throws Exception {
        // Sem token: não pode ser 401. 404 (arquivo não existe) prova que a
        // requisição passou pelo Security e chegou no ResourceHandler.
        mockMvc.perform(get("/imagens/ab/cd/inexistente.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void imagemServidaVemComCabecalhosDeSeguranca() throws Exception {
        // Conteúdo público não quer dizer conteúdo sem defesa: mesmo servindo
        // aberto, a resposta precisa impedir que o arquivo seja reinterpretado
        // como conteúdo ativo no nosso domínio.
        var png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        var arquivo = Path.of(diretorioDeImagens, "ab", "cd", "teste-headers.png");
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, png);
        try {
            mockMvc.perform(get("/imagens/ab/cd/teste-headers.png"))
                    .andExpect(status().isOk())
                    // Sem sniffing: o navegador não pode "adivinhar" outro tipo
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    // Se algo ativo escapar da allowlist de ingestão, não executa
                    .andExpect(header().string("Content-Security-Policy",
                            org.hamcrest.Matchers.containsString("sandbox")))
                    // Corta hotlinking de terceiros — banda é custo do negócio
                    .andExpect(header().string("Cross-Origin-Resource-Policy", "same-site"));
        } finally {
            Files.deleteIfExists(arquivo);
        }
    }

    @Test
    @WithMockUser
    void listaAnosDisponiveisDoMaisRecenteProMaisAntigo() throws Exception {
        mockMvc.perform(get("/questoes/anos"))
                .andExpect(status().isOk())
                // 2020 existe no banco, mas a única questão dele é anulada: um
                // ano oferecido no filtro levaria o usuário a uma lista vazia
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.contains(2023, 2022, 2021)));
    }

    @Test
    @WithMockUser
    void rotaDeAnosNaoCaiNaRotaDeId() throws Exception {
        // /questoes/anos também casa com /questoes/{id}. Se o Spring escolhesse
        // o padrão errado, "anos" viraria o id, a conversão pra Long falharia
        // e a resposta seria 400 — não a lista.
        mockMvc.perform(get("/questoes/anos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void anosExigemAutenticacao() throws Exception {
        mockMvc.perform(get("/questoes/anos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void anosNaoCarregamEntidadeNenhuma() throws Exception {
        // Projeção escalar: 1 query, e nenhuma Questao hidratada. Se alguém
        // trocar por findAll() + stream().map(getAno).distinct(), o número de
        // entidades carregadas denuncia
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        mockMvc.perform(get("/questoes/anos")).andExpect(status().isOk());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(statistics.getEntityLoadCount())
                .as("lista de anos não deve carregar entidade Questao")
                .isZero();
    }

    private Questao questao(int ano, int numero, Area area, Idioma idioma, boolean anulada) {
        var questao = new Questao();
        questao.setAno(ano);
        questao.setNumero(numero);
        questao.setArea(area);
        questao.setIdioma(idioma);
        questao.setEnunciado("Enunciado da questão %d de %d".formatted(numero, ano));
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito(anulada ? null : "A");
        questao.setAnulada(anulada);
        questao.setFonte("teste");
        questao.setFonteId(ano + "/" + numero);
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
