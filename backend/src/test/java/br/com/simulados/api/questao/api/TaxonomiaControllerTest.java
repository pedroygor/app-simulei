package br.com.simulados.api.questao.api;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class TaxonomiaControllerTest {

    // Caminho do assunto "probabilidade" no JSON: Matemática é a 2ª área
    // (índice 1) na ordem da V5, com uma matéria só
    private static final String PROBABILIDADE =
            "$[1].materias[0].assuntos[?(@.slug == 'probabilidade')].questoes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void semear() {
        questaoRepository.deleteAll();
        questaoRepository.saveAll(List.of(
                classificada(2023, 90, "probabilidade", false),
                classificada(2022, 91, "probabilidade", false),
                // Anulada: nunca entra em prova, então não pode inflar a contagem
                classificada(2021, 92, "probabilidade", true),
                classificada(2023, 93, "estatistica", false)));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @WithMockUser
    void devolveAArvoreInteiraNaOrdemDaTaxonomia() throws Exception {
        mockMvc.perform(get("/taxonomia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                // Ordem da V5 (a mesma do docs/taxonomia.md), não alfabética
                .andExpect(jsonPath("$[0].area").value("LINGUAGENS"))
                .andExpect(jsonPath("$[1].area").value("MATEMATICA"))
                .andExpect(jsonPath("$[2].area").value("CIENCIAS_NATUREZA"))
                .andExpect(jsonPath("$[3].area").value("CIENCIAS_HUMANAS"))
                .andExpect(jsonPath("$[*].materias[*]", hasSize(14)))
                .andExpect(jsonPath("$[*].materias[*].assuntos[*]", hasSize(118)));
    }

    @Test
    @WithMockUser
    void contaSoAsQuestoesValidasDeCadaAssunto() throws Exception {
        mockMvc.perform(get("/taxonomia"))
                .andExpect(status().isOk())
                // 3 de probabilidade na semente, 1 anulada -> 2
                .andExpect(jsonPath(PROBABILIDADE).value(2));
    }

    @Test
    @WithMockUser
    void assuntoSemQuestaoApareceComZero() throws Exception {
        // É o teste do LEFT JOIN com a condição no ON: se `anulada = false`
        // fosse pro WHERE, os assuntos sem questão sumiriam da árvore e o
        // total cairia de 118 pra 2
        mockMvc.perform(get("/taxonomia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].materias[0].assuntos[?(@.slug == 'trigonometria')].questoes").value(0));
    }

    @Test
    @WithMockUser
    void custaUmaQuerySo() throws Exception {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        mockMvc.perform(get("/taxonomia")).andExpect(status().isOk());

        // 118 assuntos, 14 matérias, contagem de cada um: tudo num SELECT. Um
        // laço contando por assunto seriam 118 queries
        assertThat(statistics.getPrepareStatementCount())
                .as("taxonomia com contagem deve custar 1 query")
                .isEqualTo(1);
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mockMvc.perform(get("/taxonomia")).andExpect(status().isUnauthorized());
    }

    private Questao classificada(int ano, int numero, String slugDoAssunto, boolean anulada) {
        var questao = new Questao();
        questao.setAno(ano);
        questao.setNumero(numero);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Enunciado %d/%d".formatted(numero, ano));
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito(anulada ? null : "A");
        questao.setAnulada(anulada);
        questao.setFonte("teste");
        questao.setFonteId(ano + "/" + numero);
        questao.setAssunto(entityManager
                .createQuery("select a from Assunto a where a.slug = :slug", Assunto.class)
                .setParameter("slug", slugDoAssunto)
                .getSingleResult());
        questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
        questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
