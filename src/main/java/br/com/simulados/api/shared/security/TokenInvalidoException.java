package br.com.simulados.api.shared.security;

// Não estende NegocioException: falha de autenticação não é regra de negócio,
// e o handler traduz pra 401 em vez do fallback 422
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("Refresh token inválido ou expirado");
    }
}
