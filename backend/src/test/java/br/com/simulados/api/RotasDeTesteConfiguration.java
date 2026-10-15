package br.com.simulados.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Rota que só existe nos testes: exercita a autorização por role
// (claim do JWT -> authority -> @PreAuthorize) sem expor endpoint de
// produção sem propósito de negócio.
// A classe aninhada com @RestController é registrada automaticamente quando
// esta configuração é importada — sem @Bean explícito (duplicaria o mapping).
@TestConfiguration(proxyBeanMethods = false)
public class RotasDeTesteConfiguration {

    // ADMIN não existe no enum: a rota prova que o @PreAuthorize RECUSA papel
    // que o token não tem (403), sem precisar de um segundo papel de verdade
    @RestController
    public static class RotasPorPapel {

        @GetMapping("/teste/so-admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String soAdmin() {
            return "ok";
        }

        @GetMapping("/teste/so-aluno")
        @PreAuthorize("hasRole('ALUNO')")
        public String soAluno() {
            return "ok";
        }
    }
}
