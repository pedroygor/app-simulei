package br.com.simulados.api.questao.domain;

import br.com.simulados.api.shared.exception.RecursoNaoEncontradoException;

public class AssuntoNaoEncontradoException extends RecursoNaoEncontradoException {

    public AssuntoNaoEncontradoException(String slug) {
        super("Assunto não encontrado: " + slug);
    }
}
