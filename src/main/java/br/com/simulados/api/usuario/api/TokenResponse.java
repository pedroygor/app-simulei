package br.com.simulados.api.usuario.api;

// expiresIn em segundos: o frontend usa pra agendar o refresh antes de expirar
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
