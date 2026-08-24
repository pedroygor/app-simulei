package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.NegocioException;

public class EmailJaCadastradoException extends NegocioException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário cadastrado com o e-mail " + email);
    }
}
