package br.com.simulados.api.shared.exception;

// Base semântica: "pedi algo que não existe" -> 404.
// Cada domínio estende esta classe; o GlobalExceptionHandler só conhece a base,
// então domínio novo não exige mexer no handler.
public abstract class RecursoNaoEncontradoException extends NegocioException {

    protected RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
