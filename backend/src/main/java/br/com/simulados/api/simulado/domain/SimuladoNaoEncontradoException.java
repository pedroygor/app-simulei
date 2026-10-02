package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.shared.exception.RecursoNaoEncontradoException;

import java.util.UUID;

/**
 * Vale tanto pra simulado inexistente quanto pra simulado de OUTRO usuário: os
 * dois viram o mesmo 404, com a mesma mensagem. Um 403 pro alheio confirmaria que
 * aquele id existe — vazamento pequeno, mas de graça.
 */
public class SimuladoNaoEncontradoException extends RecursoNaoEncontradoException {

    public SimuladoNaoEncontradoException(UUID id) {
        super("Simulado não encontrado: " + id);
    }
}
