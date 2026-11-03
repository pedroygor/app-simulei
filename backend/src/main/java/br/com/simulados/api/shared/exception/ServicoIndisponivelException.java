package br.com.simulados.api.shared.exception;

/**
 * Base semântica: "a culpa não é sua, um serviço que dependemos não está
 * disponível agora" -> 503. Ex.: o Google fora do ar, ou o login com Google
 * não configurado nesta instalação.
 */
public abstract class ServicoIndisponivelException extends RuntimeException {

    protected ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }

    protected ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
