package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.NegocioException;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends NegocioException {

    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado: " + id);
    }
}
