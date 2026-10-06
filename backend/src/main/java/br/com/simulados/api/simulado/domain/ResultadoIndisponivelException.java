package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.shared.exception.NegocioException;

/**
 * Pedir o resultado (e com ele o gabarito) de uma prova ainda em andamento. É a
 * regra "o aluno vê o gabarito só ao final da prova" (ADR de 01/10/2026) — 422,
 * não 404: a tentativa existe e é dele, só não acabou.
 */
public class ResultadoIndisponivelException extends NegocioException {

    public ResultadoIndisponivelException() {
        super("O resultado sai depois que a prova for entregue");
    }
}
