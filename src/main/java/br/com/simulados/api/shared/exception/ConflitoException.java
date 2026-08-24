package br.com.simulados.api.shared.exception;

// Base semântica: "payload válido, mas conflita com estado existente" -> 409
public abstract class ConflitoException extends NegocioException {

    protected ConflitoException(String mensagem) {
        super(mensagem);
    }
}
