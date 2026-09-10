package br.com.simulados.api.shared.exception;

/**
 * Base semântica: "você excedeu um limite de frequência" -> 429.
 *
 * Completa a família (RecursoNaoEncontrado -> 404, Conflito -> 409,
 * RequisicaoInvalida -> 400). O handler continua conhecendo só as bases:
 * domínio novo estende e nada muda lá.
 */
public abstract class MuitasRequisicoesException extends NegocioException {

    protected MuitasRequisicoesException(String mensagem) {
        super(mensagem);
    }
}
