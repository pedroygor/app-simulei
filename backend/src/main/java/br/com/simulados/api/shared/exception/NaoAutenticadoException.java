package br.com.simulados.api.shared.exception;

/**
 * Base semântica: "não deu pra confirmar quem você é" -> 401.
 *
 * Não estende NegocioException: falha de autenticação não é regra de negócio,
 * e cairia no fallback 422. A mensagem vai pro cliente, então não pode
 * revelar detalhe que ajude um atacante (qual checagem falhou, se a conta existe).
 */
public abstract class NaoAutenticadoException extends RuntimeException {

    protected NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
