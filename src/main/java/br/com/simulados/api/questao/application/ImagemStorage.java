package br.com.simulados.api.questao.application;

/**
 * Port de saída: o caso de uso sabe que precisa guardar imagens, mas não sabe
 * onde. Hoje o adaptador grava em disco local; em produção vira object storage
 * (S3/R2) sem que o importador mude uma linha.
 */
public interface ImagemStorage {

    /**
     * Baixa a imagem da origem, VALIDA e guarda no nosso storage.
     *
     * O adaptador é a fronteira de confiança com a fonte externa: ele decide o
     * que entra. Não recebe ano/questão de propósito — guardar imagem não
     * precisa conhecer o domínio de questões.
     *
     * @return a URL local (servível pela nossa API) da imagem guardada
     * @throws ImagemIndisponivelException quando o download falha
     * @throws ImagemRejeitadaException quando o conteúdo não passa na validação
     */
    String guardar(String urlOrigem);
}
