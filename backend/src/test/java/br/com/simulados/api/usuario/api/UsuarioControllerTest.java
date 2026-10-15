package br.com.simulados.api.usuario.api;

import br.com.simulados.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Boot 4 moveu as anotações de teste pra pacotes por módulo (antes era
// org.springframework.boot.test.autoconfigure.web.servlet)
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integração de verdade: contexto completo + Postgres real via Testcontainers.
// MockMvc dispensa subir o Tomcat, mas passa por toda a cadeia de filtros,
// validação, service e banco.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cadastraUsuarioERetorna201SemVazarSenha() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana Souza",
                                  "email": "ana@teste.com",
                                  "senha": "senha-forte-123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("ana@teste.com"))
                .andExpect(jsonPath("$.plano").value("FREE"))
                // A regra dura do projeto: hash de senha jamais aparece na resposta
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void retorna409QuandoEmailJaCadastrado() throws Exception {
        var corpo = """
                {
                  "nome": "Bruno Lima",
                  "email": "bruno@teste.com",
                  "senha": "senha-forte-123"
                }
                """;

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.path").value("/usuarios"));
    }

    // Regressão do bug de dispatch: erros fora do @Valid (JSON quebrado, enum
    // inexistente) devem ser 400 no NOSSO formato — não o 401 que o Security
    // devolvia quando o forward pro /error era bloqueado
    @Test
    void jsonMalformadoRetorna400ComCorpoPadronizado() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Ana\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido ou malformado"));
    }

    // O papel é decidido pelo servidor: quem manda "role" (cliente antigo ou
    // alguém tentando se promover) recebe ALUNO do mesmo jeito
    @Test
    void cadastroEhSempreAlunoMesmoPedindoOutroPapel() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana","email":"role.x@teste.com","senha":"senha-forte-123","role":"PROFESSOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ALUNO"));
    }

    @Test
    void retorna400ComListaDeCamposQuandoPayloadInvalido() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "email": "nao-e-email",
                                  "senha": "curta"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos.length()").value(3));
    }
}
