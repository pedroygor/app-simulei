package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.shared.exception.RecursoNaoEncontradoException;

import java.util.UUID;

// Inexistente e alheia dão o mesmo 404 (ver SimuladoNaoEncontradoException)
public class TentativaNaoEncontradaException extends RecursoNaoEncontradoException {

    public TentativaNaoEncontradaException(UUID id) {
        super("Tentativa não encontrada: " + id);
    }
}
