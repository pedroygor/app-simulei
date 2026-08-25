package br.com.simulados.api.questao.infrastructure.enemdev;

// Falha de comunicação com a enem.dev (HTTP de erro, timeout, DNS...).
// Não estende NegocioException: é problema de infraestrutura, e quem chama
// (o job de importação) decide se re-tenta, pula ou aborta
public class EnemDevClientException extends RuntimeException {

    public EnemDevClientException(Throwable causa) {
        super("Falha ao consultar a enem.dev: " + causa.getMessage(), causa);
    }
}
