package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.NaoAutenticadoException;

// Uma mensagem só pra assinatura, emissor, audiência ou validade errados: dizer
// qual checagem falhou ajudaria quem está forjando o token
public class CredencialGoogleInvalidaException extends NaoAutenticadoException {

    public CredencialGoogleInvalidaException() {
        super("Não foi possível confirmar sua conta Google. Tente entrar de novo.");
    }
}
