package br.com.simulados.api.usuario.infrastructure.google;

import br.com.simulados.api.usuario.application.VerificadorDeTokenDoGoogle;
import br.com.simulados.api.usuario.domain.LoginComGoogleIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
class GoogleConfig {

    @Bean
    VerificadorDeTokenDoGoogle verificadorDeTokenDoGoogle(@Value("${google.client-id}") String clientId,
                                                          @Value("${google.jwks-uri}") String jwksUri) {
        if (clientId.isBlank()) {
            // Sem Client ID, quem clonou o projeto roda tudo sem Google Cloud:
            // o login com Google responde 503 e o resto segue igual
            return credencial -> {
                throw new LoginComGoogleIndisponivelException();
            };
        }

        // As chaves públicas são buscadas na PRIMEIRA verificação (não ao subir)
        // e ficam em cache; o Google as troca de tempos em tempos e o decoder
        // busca de novo quando aparece um "kid" que ele não conhece.
        // Timeouts curtos: o padrão do Spring Security é 30 s pra conectar e 30 s
        // pra ler, tempo demais com a pessoa olhando a tela de login
        var http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(Duration.ofSeconds(3));
        http.setReadTimeout(Duration.ofSeconds(5));
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .restOperations(new RestTemplate(http))
                .build();
        return new VerificadorDeTokenDoGoogleNimbus(decoder, clientId);
    }
}
