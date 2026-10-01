package br.com.simulados.api.questao.application;

import java.util.List;

/**
 * O dados/conferencia-inep/habilidades.csv tem linha que não pode ser aplicada:
 * formato inválido, questão repetida, ou a área do INEP não bate com a da questão
 * (sinal de que o casamento por posição caiu no lugar errado).
 */
public class HabilidadesInvalidasException extends CargaInvalidaException {

    public HabilidadesInvalidasException(List<String> problemas) {
        super("Carga das habilidades do INEP", problemas);
    }
}
