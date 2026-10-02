package br.com.simulados.api.simulado.api;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SimuladoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private AssuntoRepository assuntoRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID aluno;

    @BeforeEach
    void setUp() {
        var usuario = new Usuario();
        usuario.setNome("Aluno");
        usuario.setEmail("aluno@exemplo.com");
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        aluno = usuarioRepository.save(usuario).getId();

        var assuntos = assuntoRepository.findAll().stream().collect(Collectors.toMap(Assunto::getSlug, a -> a));
        for (int n = 1; n <= 8; n++) {
            salvar(assuntos.get("probabilidade"), 100 + n);
            salvar(assuntos.get("estatistica"), 200 + n);
        }
    }

    @Test
    void criaAProvaE201ComLocationEComposicao() throws Exception {
        mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": "Treino de sábado", "total": 10,
                          "itens": [ { "tipo": "ASSUNTO", "valor": "probabilidade" },
                                     { "tipo": "ASSUNTO", "valor": "estatistica" } ] }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/simulados/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.titulo").value("Treino de sábado"))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.composicao[0].rotulo").value("Probabilidade"))
                .andExpect(jsonPath("$.composicao[0].quantidade").value(5))
                .andExpect(jsonPath("$.composicao[1].quantidade").value(5))
                // Sem questões nem gabarito na criação: a prova abre pela tentativa (3b)
                .andExpect(jsonPath("$.questoes").doesNotExist())
                .andExpect(jsonPath("$.gabarito").doesNotExist());
    }

    @Test
    void validaCadaCampoInclusiveDentroDasListas() throws Exception {
        mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": " ", "total": 3, "anos": [1990],
                          "itens": [ { "tipo": "ASSUNTO", "valor": "probabilidade", "quantidade": -3 } ],
                          "tempoLimiteSeg": 10 }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("titulo")))
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("total")))
                // Só pega porque a anotação está DENTRO do tipo genérico
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("anos[0]")))
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("itens[0].quantidade")))
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("tempoLimiteSeg")));
    }

    @Test
    void listaDeEscolhasVaziaEh400() throws Exception {
        mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": "x", "total": 10, "itens": [] }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("itens")));
    }

    @Test
    void valorComCaracterInvalidoEh400AntesDeChegarAoBanco() throws Exception {
        mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": "x", "total": 10, "itens": [ { "tipo": "ASSUNTO", "valor": "'; drop table x" } ] }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("itens[0].valor")));
    }

    @Test
    void tipoDesconhecidoEh400() throws Exception {
        mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": "x", "total": 10, "itens": [ { "tipo": "DISCIPLINA", "valor": "matematica" } ] }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sobreposicaoEh422ComAMensagemDoService() throws Exception {
        mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": "x", "total": 10,
                          "itens": [ { "tipo": "MATERIA", "valor": "matematica" },
                                     { "tipo": "ASSUNTO", "valor": "probabilidade" } ] }
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Probabilidade já está dentro de Matemática: escolha um dos dois"));
    }

    @Test
    void faltaDeQuestaoEh422DizendoQuantoPediuEQuantoHa() throws Exception {
        mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": "x", "total": 20, "itens": [ { "tipo": "ASSUNTO", "valor": "probabilidade" } ] }
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Não há questões suficientes: pediu 20 questões, há 8 nas escolhas e anos pedidos"));
    }

    @Test
    void donoVeOSimuladoComAComposicaoEmDuasQueries() throws Exception {
        var id = criarProva();
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        mockMvc.perform(comoAluno(get("/simulados/{id}", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.composicao[0].area").value("MATEMATICA"))
                // Na ordem da taxonomia (V7), não na do pedido: estatística vem antes
                .andExpect(jsonPath("$.composicao[0].assunto.slug").value("estatistica"))
                .andExpect(jsonPath("$.composicao[0].questoes").value(5))
                .andExpect(jsonPath("$.composicao[1].assunto.slug").value("probabilidade"))
                // A prova abre pela tentativa (3b): aqui não vão questões nem gabarito
                .andExpect(jsonPath("$.questoes").doesNotExist());

        // O simulado com as questões (@EntityGraph, filtrado pelo dono) + a composição
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void simuladoDeOutroUsuarioEh404IgualAoInexistente() throws Exception {
        var id = criarProva();

        var outro = new Usuario();
        outro.setNome("Outra pessoa");
        outro.setEmail("outra@exemplo.com");
        outro.setSenhaHash("hash-irrelevante-aqui");
        outro.setRole(Role.PROFESSOR);
        var outroId = usuarioRepository.save(outro).getId();

        mockMvc.perform(get("/simulados/{id}", id)
                        .with(jwt().jwt(j -> j.subject(outroId.toString())).authorities(new SimpleGrantedAuthority("ROLE_PROFESSOR"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Simulado não encontrado: " + id));
        mockMvc.perform(comoAluno(get("/simulados/{id}", UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void idQueNaoEhUuidEh400() throws Exception {
        mockMvc.perform(comoAluno(get("/simulados/{id}", "123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void semTokenEh401() throws Exception {
        mockMvc.perform(post("/simulados").contentType(MediaType.APPLICATION_JSON).content("""
                        { "titulo": "x", "total": 10, "itens": [ { "tipo": "ASSUNTO", "valor": "probabilidade" } ] }
                        """))
                .andExpect(status().isUnauthorized());
    }

    // --- apoio ---

    private String criarProva() throws Exception {
        var resposta = mockMvc.perform(comoAluno(post("/simulados")).content("""
                        { "titulo": "Treino", "total": 10,
                          "itens": [ { "tipo": "ASSUNTO", "valor": "probabilidade" },
                                     { "tipo": "ASSUNTO", "valor": "estatistica" } ] }
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    // jwt(): token já validado, sem assinar nada — o `sub` é o id do usuário, como
    // o JwtService gera de verdade
    private MockHttpServletRequestBuilder comoAluno(MockHttpServletRequestBuilder requisicao) {
        return requisicao.contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt(j -> j.subject(aluno.toString())).authorities(new SimpleGrantedAuthority("ROLE_ALUNO")));
    }

    private void salvar(Assunto assunto, int numero) {
        var questao = new Questao();
        questao.setAno(2023);
        questao.setNumero(numero);
        questao.setArea(assunto.getArea());
        questao.setAssunto(assunto);
        questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
        questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2023/" + numero);
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        questaoRepository.save(questao);
    }
}
