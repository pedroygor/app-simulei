package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.shared.exception.NegocioException;

/**
 * Responder ou desmarcar depois do prazo da prova (3d). 422: a tentativa existe e
 * é do aluno, só que o tempo dela acabou. Quem decide é o relógio do SERVIDOR —
 * o do celular o aluno ajusta.
 */
public class TempoEsgotadoException extends NegocioException {

    public TempoEsgotadoException() {
        super("O tempo da prova acabou");
    }
}
