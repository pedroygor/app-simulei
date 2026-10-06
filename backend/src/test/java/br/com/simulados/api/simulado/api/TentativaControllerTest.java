package br.com.simulados.api.simulado.api;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class TentativaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID aluno;
    private UUID outro;
    private UUID simulado;
    private Questao q1;
    private Questao q2;

    @BeforeEach
    void setUp() {
        aluno = usuario("aluno@exemplo.com");
        outro = usuario("outro@exemplo.com");
        q1 = questaoRepository.save(questao(136));
        q2 = questaoRepository.save(questao(137));
        // q2 antes de q1: a prova segue a ordem do simulado, não a dos ids
        simulado = simuladoRepository.save(new Simulado(aluno, "Treino", 3600, List.of(q2.getId(), q1.getId()))).getId();
        entityManager.flush();
    }

    // --- iniciar ---

    @Test
    void iniciarCria201EDepoisDevolveAMesmaCom200() throws Exception {
        var id = iniciar();

        mockMvc.perform(como(aluno, post("/simulados/{id}/tentativas", simulado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    void simuladoAlheioEh404() throws Exception {
        mockMvc.perform(como(outro, post("/simulados/{id}/tentativas", simulado)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Simulado não encontrado: " + simulado));
    }

    // --- a prova ---

    @Test
    void provaVemNaOrdemDoSimuladoSemGabaritoNemDica() throws Exception {
        var id = iniciar();

        mockMvc.perform(como(aluno, get("/tentativas/{id}", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Treino"))
                .andExpect(jsonPath("$.tempoLimiteSeg").value(3600))
                .andExpect(jsonPath("$.questoes.length()").value(2))
                .andExpect(jsonPath("$.questoes[0].ordem").value(1))
                .andExpect(jsonPath("$.questoes[0].numero").value(137))
                .andExpect(jsonPath("$.questoes[1].numero").value(136))
                .andExpect(jsonPath("$.questoes[0].alternativas.length()").value(5))
                .andExpect(jsonPath("$.questoes[0].alternativas[0].letra").value("A"))
                .andExpect(jsonPath("$.questoes[0].marcada").isEmpty())
                .andExpect(jsonPath("$..gabarito").isEmpty())
                .andExpect(jsonPath("$..correta").isEmpty())
                .andExpect(jsonPath("$..assunto").isEmpty())
                .andExpect(jsonPath("$..habilidade").isEmpty());
    }

    @Test
    void questaoAnuladaDepoisDaMontagemSomeDaProvaQueERenumerada() throws Exception {
        var id = iniciar();
        // q2 era a 1ª da prova; o INEP a anula depois da montagem
        entityManager.createNativeQuery("UPDATE questoes SET anulada = true, gabarito = NULL WHERE id = :id")
                .setParameter("id", q2.getId())
                .executeUpdate();
        entityManager.clear();

        mockMvc.perform(como(aluno, get("/tentativas/{id}", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questoes.length()").value(1))
                .andExpect(jsonPath("$.questoes[0].numero").value(136))
                .andExpect(jsonPath("$.questoes[0].ordem").value(1));
    }

    @Test
    void provaCustaQuatroQueriesQualquerQueSejaOTamanho() throws Exception {
        var id = iniciar();
        entityManager.clear();
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        mockMvc.perform(como(aluno, get("/tentativas/{id}", id))).andExpect(status().isOk());

        // tentativa + simulado com a ordem + questões com alternativas + respostas
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    // --- responder e desmarcar ---

    @Test
    void responderTrocarEDesmarcarAparecemNaProva() throws Exception {
        var id = iniciar();

        responder(id, q1, "B").andExpect(status().isNoContent());
        responder(id, q1, "C").andExpect(status().isNoContent());
        mockMvc.perform(como(aluno, get("/tentativas/{id}", id)))
                .andExpect(jsonPath("$.questoes[1].marcada").value(alternativa(q1, "C")))
                .andExpect(jsonPath("$.questoes[0].marcada").isEmpty());

        mockMvc.perform(como(aluno, delete("/tentativas/{id}/respostas/{q}", id, q1.getId())))
                .andExpect(status().isNoContent());
        mockMvc.perform(como(aluno, get("/tentativas/{id}", id)))
                .andExpect(jsonPath("$.questoes[1].marcada").isEmpty());
    }

    @Test
    void corpoSemAlternativaOuComIdInvalidoEh400() throws Exception {
        var id = iniciar();

        for (var corpo : List.of("{}", "{ \"alternativaId\": -1 }")) {
            mockMvc.perform(como(aluno, put("/tentativas/{id}/respostas/{q}", id, q1.getId())).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[*].campo").value(hasItem("alternativaId")));
        }
    }

    @Test
    void alternativaDeOutraQuestaoEh422() throws Exception {
        var id = iniciar();

        mockMvc.perform(como(aluno, put("/tentativas/{id}/respostas/{q}", id, q1.getId()))
                        .content("{ \"alternativaId\": " + alternativa(q2, "A") + " }"))
                .andExpect(status().isUnprocessableContent());
    }

    // --- finalizar ---

    @Test
    void finalizarEntregaEDepoisNadaMuda() throws Exception {
        var id = iniciar();
        responder(id, q1, "A");

        mockMvc.perform(como(aluno, post("/tentativas/{id}/finalizar", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.finalizadoEm").isNotEmpty());

        responder(id, q2, "A")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Esta prova já foi finalizada e não aceita mais respostas"));
        mockMvc.perform(como(aluno, post("/tentativas/{id}/finalizar", id)))
                .andExpect(status().isUnprocessableContent());
    }

    // --- dono e formato ---

    @Test
    void tentativaAlheiaEh404EmTudo() throws Exception {
        var id = iniciar();

        mockMvc.perform(como(outro, get("/tentativas/{id}", id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Tentativa não encontrada: " + id));
        mockMvc.perform(como(outro, put("/tentativas/{id}/respostas/{q}", id, q1.getId()))
                        .content("{ \"alternativaId\": " + alternativa(q1, "A") + " }"))
                .andExpect(status().isNotFound());
        mockMvc.perform(como(outro, delete("/tentativas/{id}/respostas/{q}", id, q1.getId())))
                .andExpect(status().isNotFound());
        mockMvc.perform(como(outro, post("/tentativas/{id}/finalizar", id)))
                .andExpect(status().isNotFound());
    }

    @Test
    void idQueNaoEhUuidEh400ESemTokenEh401() throws Exception {
        mockMvc.perform(como(aluno, get("/tentativas/{id}", "123")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/tentativas/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // --- apoio ---

    private String iniciar() throws Exception {
        var corpo = mockMvc.perform(como(aluno, post("/simulados/{id}/tentativas", simulado)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/tentativas/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.id");
    }

    private ResultActions responder(String tentativa, Questao questao, String letra)
            throws Exception {
        return mockMvc.perform(como(aluno, put("/tentativas/{id}/respostas/{q}", tentativa, questao.getId()))
                .content("{ \"alternativaId\": " + alternativa(questao, letra) + " }"));
    }

    // jwt(): token já validado, sem assinar nada — o `sub` é o id do usuário
    private static MockHttpServletRequestBuilder como(UUID usuario, MockHttpServletRequestBuilder requisicao) {
        return requisicao.contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt(j -> j.subject(usuario.toString())).authorities(new SimpleGrantedAuthority("ROLE_ALUNO")));
    }

    private UUID usuario(String email) {
        var usuario = new Usuario();
        usuario.setNome("Aluno");
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

    private static Questao questao(int numero) {
        var questao = new Questao();
        questao.setAno(2020);
        questao.setNumero(numero);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2020/" + numero);
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
