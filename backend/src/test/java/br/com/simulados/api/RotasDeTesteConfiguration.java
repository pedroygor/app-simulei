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

    @RestController
    public static class RotaSoProfessor {

        @GetMapping("/teste/so-professor")
        @PreAuthorize("hasRole('PROFESSOR')")
        public String soProfessor() {
            return "ok";
        }
    }
}
