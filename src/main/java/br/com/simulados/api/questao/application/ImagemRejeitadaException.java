package br.com.simulados.api.questao.application;

/**
 * A fonte entregou um arquivo que não aceitamos guardar (formato fora da
 * allowlist, ou grande demais). Distinta de ImagemIndisponivelException, que é
 * falha de rede: aqui o download funcionou e nós é que recusamos.
 *
 * O importador trata as duas do mesmo jeito — mantém a URL da fonte e segue —
 * mas separar deixa o log honesto sobre o que aconteceu.
 */
public class ImagemRejeitadaException extends RuntimeException {

    public ImagemRejeitadaException(String url, String motivo) {
        super("Imagem recusada (%s): %s".formatted(motivo, url));
    }
}
