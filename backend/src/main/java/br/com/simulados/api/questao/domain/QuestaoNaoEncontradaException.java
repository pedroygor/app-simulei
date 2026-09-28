package br.com.simulados.api.questao.domain;

import br.com.simulados.api.shared.exception.RecursoNaoEncontradoException;

// Estende a base semântica do shared: o GlobalExceptionHandler já traduz
// RecursoNaoEncontradoException pra 404 e não precisa saber que este domínio
// existe. É o refactor da revisão pós-Etapa 1 pagando dividendo.
public class QuestaoNaoEncontradaException extends RecursoNaoEncontradoException {

    public QuestaoNaoEncontradaException(Long id) {
        super("Questão não encontrada: " + id);
    }
}
