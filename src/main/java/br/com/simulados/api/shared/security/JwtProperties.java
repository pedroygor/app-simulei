package br.com.simulados.api.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Liga o bloco "jwt:" do application.yml a um record tipado.
// O Boot converte "30m"/"7d" em Duration sozinho.
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Duration accessTtl,
        Duration refreshTtl
) {
}
