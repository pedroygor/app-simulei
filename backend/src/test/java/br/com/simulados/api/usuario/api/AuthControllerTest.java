package br.com.simulados.api.usuario.api;

import br.com.simulados.api.RotasDeTesteConfiguration;
import br.com.simulados.api.TestcontainersConfiguration;
// Jackson 3 (Boot 4): o pacote mudou de com.fasterxml.jackson pra tools.jackson
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fluxo completo de auth contra Postgres real: cadastro -> login -> acesso
// com Bearer -> refresh via cookie -> autorização por role
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RotasDeTesteConfiguration.class})
class AuthControllerTest {

    private static final String SENHA = "senha-forte-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginDevolveAccessTokenECookieHttpOnlyDeRefresh() throws Exception {
        cadastrar("Ana", "auth.ana@teste.com", "ALUNO");

        var resposta = login("auth.ana@teste.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andReturn();

        var setCookie = resposta.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refresh_token=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/auth/refresh");
    }

    @Test
    void loginComSenhaErradaDevolve401SemRevelarSeEmailExiste() throws Exception {
        cadastrar("Bia", "auth.bia@teste.com", "ALUNO");

        login("auth.bia@teste.com", "senha-errada-999")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));

        // E-mail inexistente: mesma resposta, mesmo status
        login("ninguem@teste.com", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void endpointProtegidoExigeToken() throws Exception {
        mockMvc.perform(get("/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoDevolveUsuarioDoToken() throws Exception {
        cadastrar("Caio", "auth.caio@teste.com", "ALUNO");
        var accessToken = extrairAccessToken(login("auth.caio@teste.com", SENHA).andReturn());

        mockMvc.perform(get("/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("auth.caio@teste.com"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void rotaDeProfessorRejeitaAlunoCom403() throws Exception {
        cadastrar("Duda", "auth.duda@teste.com", "ALUNO");
        cadastrar("Edu", "auth.edu@teste.com", "PROFESSOR");

        var tokenAluno = extrairAccessToken(login("auth.duda@teste.com", SENHA).andReturn());
        var tokenProfessor = extrairAccessToken(login("auth.edu@teste.com", SENHA).andReturn());

        mockMvc.perform(get("/teste/so-professor").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAluno))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/teste/so-professor").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProfessor))
                .andExpect(status().isOk());
    }

    @Test
    void refreshComCookieDevolveNovoAccessToken() throws Exception {
        cadastrar("Fabi", "auth.fabi@teste.com", "ALUNO");
        var cookieRefresh = extrairCookieRefresh(login("auth.fabi@teste.com", SENHA).andReturn());

        var novoToken = extrairAccessToken(mockMvc.perform(post("/auth/refresh").cookie(cookieRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn());

        // O access token novo funciona de verdade
        mockMvc.perform(get("/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + novoToken))
                .andExpect(status().isOk());
    }

    @Test
    void refreshSemCookieOuComTokenInvalidoDevolve401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", "token-invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenNaoServeComoRefreshERefreshNaoServeComoAccess() throws Exception {
        cadastrar("Gil", "auth.gil@teste.com", "ALUNO");
        var resultado = login("auth.gil@teste.com", SENHA).andReturn();
        var accessToken = extrairAccessToken(resultado);
        var cookieRefresh = extrairCookieRefresh(resultado);

        // Access token no lugar do refresh: recusado
        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", accessToken)))
                .andExpect(status().isUnauthorized());

        // Refresh token no header Authorization: recusado pelo resource server
        mockMvc.perform(get("/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cookieRefresh.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExpiraOCookieDeRefresh() throws Exception {
        cadastrar("Hugo", "auth.hugo@teste.com", "ALUNO");
        var cookieRefresh = extrairCookieRefresh(login("auth.hugo@teste.com", SENHA).andReturn());

        var resposta = mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        // Max-Age=0 manda o navegador apagar o cookie. Importa no computador
        // compartilhado do cursinho: sem isso o próximo aluno retomaria a sessão
        var setCookie = resposta.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refresh_token=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("Path=/auth/refresh");

        // O token em si segue válido (é stateless) — o que muda é o navegador
        // não tê-lo mais. Documentado como limitação consciente.
        mockMvc.perform(post("/auth/refresh").cookie(cookieRefresh))
                .andExpect(status().isOk());
    }

    @Test
    void bloqueiaLoginDepoisDeVariasTentativasErradas() throws Exception {
        // E-mail exclusivo deste teste: o limitador é singleton e guarda estado
        // entre testes da mesma suíte
        var alvo = "auth.brute@teste.com";
        cadastrar("Alvo", alvo, "ALUNO");

        // 5 falhas seguidas: as primeiras devolvem 401
        for (var i = 0; i < 5; i++) {
            login(alvo, "senha-errada-" + i).andExpect(status().isUnauthorized());
        }

        // A partir daí é 429, no nosso formato de erro
        login(alvo, "senha-errada-final")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("Muitas tentativas")));

        // E a senha CERTA também é barrada — senão o bloqueio não valeria nada
        login(alvo, SENHA).andExpect(status().isTooManyRequests());
    }

    @Test
    void bloqueioDeUmaContaNaoAfetaOutra() throws Exception {
        // A turma do cursinho sai pelo mesmo IP: um aluno errando a senha não
        // pode derrubar o login dos colegas. Por isso limitamos por conta.
        var azarado = "auth.azarado@teste.com";
        var colega = "auth.colega@teste.com";
        cadastrar("Azarado", azarado, "ALUNO");
        cadastrar("Colega", colega, "ALUNO");

        for (var i = 0; i < 5; i++) {
            login(azarado, "errada-" + i).andExpect(status().isUnauthorized());
        }
        login(azarado, SENHA).andExpect(status().isTooManyRequests());

        // O colega entra normalmente
        login(colega, SENHA).andExpect(status().isOk());
    }

    private void cadastrar(String nome, String email, String role) throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "email": "%s", "senha": "%s", "role": "%s"}
                                """.formatted(nome, email, SENHA, role)))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "%s"}
                        """.formatted(email, senha)));
    }

    private String extrairAccessToken(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("accessToken").asString();
    }

    // O cookie chega como header Set-Cookie cru; extraímos o valor pra
    // reenviá-lo como cookie de request
    private Cookie extrairCookieRefresh(MvcResult resultado) {
        var setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        var valor = setCookie.substring("refresh_token=".length(), setCookie.indexOf(';'));
        return new Cookie("refresh_token", valor);
    }
}
