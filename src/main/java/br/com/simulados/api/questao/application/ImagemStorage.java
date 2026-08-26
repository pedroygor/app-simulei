package br.com.simulados.api.questao.application;

/**
 * Port de saída: o caso de uso de importação sabe que precisa guardar imagens,
 * mas não sabe onde. Hoje o adaptador grava em disco local; amanhã pode ser S3
 * sem que o importador mude.
 */
public interface ImagemStorage {

    /**
     * Baixa a imagem da origem e guarda no nosso storage. Idempotente: se o
     * arquivo já existe, não baixa de novo.
     *
     * @return a URL local (servível pela nossa API) da imagem guardada
     * @throws ImagemIndisponivelException quando o download falha
     */
    String guardar(String urlOrigem, int ano, int numeroQuestao);
}
