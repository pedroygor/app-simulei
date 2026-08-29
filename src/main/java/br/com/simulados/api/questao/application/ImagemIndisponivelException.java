package br.com.simulados.api.questao.application;

// Falha ao baixar uma imagem da fonte. Não é NegocioException: é problema de
// infraestrutura, e o importador decide o que fazer (hoje: manter a URL
// original e seguir, em vez de perder a questão inteira por causa de uma imagem)
public class ImagemIndisponivelException extends RuntimeException {

    public ImagemIndisponivelException(String url, Throwable causa) {
        super("Falha ao baixar imagem: " + url, causa);
    }
}
