package br.com.simulados.api.questao.application;

import br.com.simulados.api.shared.exception.RequisicaoInvalidaException;

import java.util.List;

/**
 * O arquivo de classificação tem linha que não pode entrar no banco (slug
 * inexistente, assunto de outra área, questão repetida, formato quebrado).
 *
 * Diferente da QuestaoInvalidaException da importação — que pula a questão
 * ruim e segue —, aqui a carga inteira é recusada ANTES de gravar qualquer
 * coisa: um CSV com erro é um CSV que precisa ser corrigido, e carga pela
 * metade deixaria o banco num estado que ninguém consegue descrever.
 *
 * Carrega TODOS os problemas, não só o primeiro: corrigir um por rodada seria
 * tortura com 2.741 linhas.
 */
public class ClassificacaoInvalidaException extends RequisicaoInvalidaException {

    private static final int MAXIMO_NA_MENSAGEM = 20;

    private final List<String> problemas;

    public ClassificacaoInvalidaException(List<String> problemas) {
        super(mensagem(problemas));
        this.problemas = List.copyOf(problemas);
    }

    public List<String> getProblemas() {
        return problemas;
    }

    private static String mensagem(List<String> problemas) {
        var mostrados = problemas.stream().limit(MAXIMO_NA_MENSAGEM).toList();
        var resto = problemas.size() - mostrados.size();
        return "Classificação recusada, %d problema(s):%n  %s%s".formatted(
                problemas.size(),
                String.join(System.lineSeparator() + "  ", mostrados),
                resto > 0 ? "%n  ... e mais %d".formatted(resto) : "");
    }
}
