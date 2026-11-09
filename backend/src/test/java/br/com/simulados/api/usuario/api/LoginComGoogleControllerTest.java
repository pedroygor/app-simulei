package br.com.simulados.api.usuario.api;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.usuario.application.IdentidadeGoogle;
import br.com.simulados.api.usuario.application.VerificadorDeTokenDoGoogle;
import br.com.simulados.api.usuario.domain.CredencialGoogleInvalidaException;
import br.com.simulados.api.usuario.domain.LoginComGoogleIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O login com Google de ponta a ponta (HTTP, segurança, validação, caso de uso,
 * Postgres real), com o verificador trocado por um dublê: conferir o token de
 * verdade é o VerificadorDeTokenDoGoogleNimbusTest. Cada teste usa e-mail e
 * "sub" próprios, porque os dados ficam no banco entre os testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginComGoogleControllerTest {

    private static final String SENHA = "senha-forte-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    // Substitui o bean do GoogleConfig no contexto deste teste
    @MockitoBean
    private VerificadorDeTokenDoGoogle verificador;

    @Test
    void primeiroAcessoPedeCadastroEDepoisEntra() throws Exception {
        googleDiz("cred-carla", "sub-carla", "google.carla@teste.com", true);

        // Sem conta: 404 no nosso formato, o sinal pro front abrir o cadastro
        entrar("cred-carla")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(containsString("Ainda não existe conta")));

        var resposta = cadastrar("cred-carla", "Carla Souza")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andReturn();

        // O access token é o de sempre: abre as rotas protegidas
        var accessToken = objectMapper.readTree(resposta.getResponse().getContentAsString())
                .get("accessToken").asString();
        mockMvc.perform(get("/usuarios/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("google.carla@teste.com"))
                .andExpect(jsonPath("$.nome").value("Carla Souza"));

        // A conta nasceu sem senha, ligada ao Google
        var linha = jdbc.queryForMap(
                "SELECT senha_hash, google_sub FROM usuarios WHERE email = 'google.carla@teste.com'");
        assertThat(linha.get("senha_hash")).isNull();
        assertThat(linha.get("google_sub")).isEqualTo("sub-carla");

        entrar("cred-carla").andExpect(status().isOk());
    }

    @Test
    void contaCriadaComSenhaPassaAEntrarDosDoisJeitos() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Dani", "email": "google.dani@teste.com", "senha": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated());
        googleDiz("cred-dani", "sub-dani", "google.dani@teste.com", true);

        entrar("cred-dani").andExpect(status().isOk());

        assertThat(jdbc.queryForObject(
                "SELECT google_sub FROM usuarios WHERE email = 'google.dani@teste.com'", String.class))
                .isEqualTo("sub-dani");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "google.dani@teste.com", "senha": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void emailSemConfirmacaoDoGoogleNaoTomaContaExistente() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Edu", "email": "google.edu@teste.com", "senha": "%s"}
                                """.formatted(SENHA)))
                .andExpect(status().isCreated());
        googleDiz("cred-intruso", "sub-intruso", "google.edu@teste.com", false);

        entrar("cred-intruso").andExpect(status().isConflict());

        assertThat(jdbc.queryForObject(
                "SELECT google_sub FROM usuarios WHERE email = 'google.edu@teste.com'", String.class))
                .isNull();
    }

    @Test
    void tokenRecusadoEh401() throws Exception {
        when(verificador.verificar("cred-forjada")).thenThrow(new CredencialGoogleInvalidaException());

        entrar("cred-forjada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void googleIndisponivelEh503() throws Exception {
        when(verificador.verificar("cred-sem-google")).thenThrow(new LoginComGoogleIndisponivelException());

        entrar("cred-sem-google")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value(containsString("e-mail e senha")));
    }

    @Test
    void validaOCorpoDasDuasRotas() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("credencial")));

        cadastrar("cred-qualquer", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("nome")));

        cadastrar("cred-qualquer", "x".repeat(121))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("nome")));
    }

    private void googleDiz(String credencial, String sub, String email, boolean emailVerificado) {
        when(verificador.verificar(credencial)).thenReturn(new IdentidadeGoogle(sub, email, emailVerificado, "Nome no Google"));
    }

    private ResultActions entrar(String credencial) throws Exception {
        return mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"credencial": "%s"}
                        """.formatted(credencial)));
    }

    private ResultActions cadastrar(String credencial, String nome) throws Exception {
        return mockMvc.perform(post("/auth/google/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"credencial": "%s", "nome": "%s"}
                        """.formatted(credencial, nome)));
    }
}
