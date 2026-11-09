package br.com.simulados.api.shared.security;

import br.com.simulados.api.shared.exception.NaoAutenticadoException;

public class TokenInvalidoException extends NaoAutenticadoException {

    public TokenInvalidoException() {
        super("Refresh token inválido ou expirado");
    }
}
