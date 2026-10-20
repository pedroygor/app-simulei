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

import java.util.List;

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
                .andExpect(jsonPath("$.geral.pertoDoChute").value(false))
                .andExpect(jsonPath("$.porAssunto").isEmpty())
                .andExpect(jsonPath("$.pontosFracos").isEmpty());
    }

    @Test
    void evolucaoTemPadraoDeQuatroSemanasEValidaOLimite() throws Exception {
        var usuario = new Usuario();
        usuario.setNome("Aluna");
        usuario.setEmail("evolucao@exemplo.com");
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        var id = usuarioRepository.save(usuario).getId();
        var token = jwt().jwt(j -> j.subject(id.toString())).authorities(new SimpleGrantedAuthority("ROLE_ALUNO"));

        mockMvc.perform(get("/desempenho/evolucao").with(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amostraMinima").value(5))
                .andExpect(jsonPath("$.semanas.length()").value(4))
                .andExpect(jsonPath("$.semanas[0].inicio").isString())
                .andExpect(jsonPath("$.semanas[0].geral.percentual").value(nullValue()))
                .andExpect(jsonPath("$.semanas[0].porArea").isEmpty());
        mockMvc.perform(get("/desempenho/evolucao").param("semanas", "12").with(token))
                .andExpect(jsonPath("$.semanas.length()").value(12));
        for (var invalido : List.of("0", "53")) {
            mockMvc.perform(get("/desempenho/evolucao").param("semanas", invalido).with(token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos[0].campo").value("semanas"));
        }
        mockMvc.perform(get("/desempenho/evolucao").param("assunto", "probabilidade").with(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assunto.assunto.nome").value("Probabilidade"))
                .andExpect(jsonPath("$.assunto.materia.slug").value("matematica"))
                .andExpect(jsonPath("$.assunto.area").value("MATEMATICA"))
                .andExpect(jsonPath("$.semanas.length()").value(4));
        mockMvc.perform(get("/desempenho/evolucao").with(token))
                .andExpect(jsonPath("$.assunto").value(nullValue()));
        mockMvc.perform(get("/desempenho/evolucao").param("assunto", "Nao Valido!").with(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("assunto"));
        mockMvc.perform(get("/desempenho/evolucao").param("assunto", "nao-existe").with(token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Assunto não encontrado: nao-existe"));
        mockMvc.perform(get("/desempenho/evolucao")).andExpect(status().isUnauthorized());
    }
}
