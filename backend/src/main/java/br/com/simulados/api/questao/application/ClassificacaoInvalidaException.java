package br.com.simulados.api.questao.application;

import java.util.List;

/**
 * O arquivo de classificação tem linha que não pode entrar no banco (slug
 * inexistente, assunto de outra área, questão repetida, formato quebrado).
 *
 * Diferente da QuestaoInvalidaException da importação — que pula a questão ruim
 * e segue —, aqui a carga inteira é recusada ANTES de gravar qualquer coisa.
 */
public class ClassificacaoInvalidaException extends CargaInvalidaException {

    public ClassificacaoInvalidaException(List<String> problemas) {
        super("Classificação", problemas);
    }
}
