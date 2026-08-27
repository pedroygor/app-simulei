package br.com.simulados.api.shared.exception;

// Raiz das exceções de regra de negócio. Não conhece HTTP: quem traduz
// pra status code é o GlobalExceptionHandler, num lugar só.
public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String mensagem) {
        super(mensagem);
    }
}
