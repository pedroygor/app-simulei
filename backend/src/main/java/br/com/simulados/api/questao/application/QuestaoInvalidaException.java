package br.com.simulados.api.questao.application;

// A fonte entregou uma questão que não dá pra usar num simulado (alternativas
// em branco, número de alternativas errado, sem texto nenhum). Não é erro
// nosso nem do usuário: é qualidade de dado da origem. O importador captura,
// conta e segue — uma questão ruim não pode abortar a importação de um ano.
public class QuestaoInvalidaException extends RuntimeException {

    public QuestaoInvalidaException(int ano, int numero, String motivo) {
        super("Questão %d/%d ignorada: %s".formatted(ano, numero, motivo));
    }
}
