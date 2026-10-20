package br.com.simulados.api.desempenho.api;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O contrato HTTP. As contas em si estão no ConsultaDeDesempenhoTest; aqui, o
 * formato do JSON e quem pode chamar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class DesempenhoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void semTokenEh401() throws Exception {
        mockMvc.perform(get("/desempenho"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void alunoSemProvaRecebeOEsqueletoComPercentualNulo() throws Exception {
        var usuario = new Usuario();
        usuario.setNome("Aluna");
        usuario.setEmail("aluna@exemplo.com");
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        var id = usuarioRepository.save(usuario).getId();

        mockMvc.perform(get("/desempenho")
                        .with(jwt().jwt(j -> j.subject(id.toString())).authorities(new SimpleGrantedAuthority("ROLE_ALUNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provas").value(0))
                .andExpect(jsonPath("$.amostraMinima").value(5))
                .andExpect(jsonPath("$.geral.respondidas").value(0))
                .andExpect(jsonPath("$.geral.emBranco").value(0))
                .andExpect(jsonPath("$.geral.percentual").value(nullValue()))
                .andExpect(jsonPath("$.geral.amostraSuficiente").value(false))
                .andExpect(jsonPath("$.porArea").isEmpty())
                .andExpect(jsonPath("$.porAssunto").isEmpty());
    }
}
