package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.ConflitoException;

public class EmailJaCadastradoException extends ConflitoException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário cadastrado com o e-mail " + email);
    }
}
