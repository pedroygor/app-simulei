package br.com.simulados.api.config;

import br.com.simulados.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// O frontend em localhost:3000 falando com a API em localhost:8080 é
// cross-origin. Sem CORS o navegador bloqueia antes mesmo da resposta chegar.
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, br.com.simulados.api.RotasDeTesteConfiguration.class})
class SegurancaWebTest {

    private static final String ORIGEM_DO_FRONTEND = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightDaOrigemDoFrontendEhAceito() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DO_FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_FRONTEND))
                // Sem isto o cookie httpOnly do refresh não viaja
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void origemDesconhecidaEhRecusada() throws Exception {
        // Regra 8.1: lista explícita, nunca "*" — e com allowCredentials o
        // navegador nem aceitaria o curinga
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://site-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exponhoLocationParaOFrontendLerODoCadastro() throws Exception {
        mockMvc.perform(options("/usuarios")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DO_FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Location"));
    }

    @Test
    void semTokenDevolve401NoNossoFormatoJson() throws Exception {
        // Antes esta resposta vinha com CORPO VAZIO: o filtro do Security
        // responde antes de chegar no GlobalExceptionHandler. O frontend teria
        // que tratar dois formatos de erro diferentes.
        mockMvc.perform(get("/questoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Unauthorized"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/questoes"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void tokenInvalidoDevolve401NoNossoFormatoJson() throws Exception {
        mockMvc.perform(get("/questoes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ALUNO")
    void semPermissaoDevolve403NoNossoFormatoJson() throws Exception {
        mockMvc.perform(get("/teste/so-professor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("Forbidden"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }
}
