package br.com.simulados.api.questao.application;

import java.util.List;

// O questoes_incompletas.csv tem linha mal formada ou questão repetida
public class IncompletasInvalidasException extends CargaInvalidaException {

    public IncompletasInvalidasException(List<String> problemas) {
        super("Marcação das questões incompletas", problemas);
    }
}
