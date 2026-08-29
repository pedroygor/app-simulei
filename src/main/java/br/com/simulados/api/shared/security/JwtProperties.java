package br.com.simulados.api.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// Liga o bloco "jwt:" do application.yml a um record tipado.
// O Boot converte "30m"/"7d" em Duration sozinho.
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Duration accessTtl,
        Duration refreshTtl
) {
    // Fail fast: segredo curto derrubaria só o PRIMEIRO login (500 em runtime).
    // Validando aqui, a aplicação nem sobe — e o erro diz como resolver.
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET precisa ter pelo menos 32 bytes pro HS256. "
                            + "Gere um com: openssl rand -base64 64 | tr -d '\\n'");
        }
    }

    // Único lugar que deriva a chave criptográfica do segredo. Encoder e os
    // dois decoders (resource server e refresh) usam esta mesma origem.
    public SecretKey chaveHmac() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
