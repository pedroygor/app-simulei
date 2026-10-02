package br.com.simulados.api.questao.application;

import java.util.List;

/**
 * O arquivo de conferência com o INEP (dados/conferencia-inep/correcoes.csv)
 * tem linha que não pode ser aplicada: tipo desconhecido, letra fora de A–E,
 * valor de anulação que não é X, ou a mesma questão duas vezes.
 */
public class ConferenciaInvalidaException extends CargaInvalidaException {

    public ConferenciaInvalidaException(List<String> problemas) {
        super("Conferência com o INEP", problemas);
    }
}
