package br.com.simulados.api.shared.exception;

// Base semântica: "o pedido em si está malformado" -> 400. Completa a família
// já existente (RecursoNaoEncontrado -> 404, Conflito -> 409), pros casos que
// o Bean Validation não pega por não serem um campo do DTO.
public abstract class RequisicaoInvalidaException extends NegocioException {

    protected RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
