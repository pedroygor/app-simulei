package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.RecursoNaoEncontradoException;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends RecursoNaoEncontradoException {

    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado: " + id);
    }
}
