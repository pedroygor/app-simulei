package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.ServicoIndisponivelException;

public class LoginComGoogleIndisponivelException extends ServicoIndisponivelException {

    // Instalação sem GOOGLE_CLIENT_ID (quem clona o projeto roda sem Google Cloud)
    public LoginComGoogleIndisponivelException() {
        super("O login com Google não está disponível nesta instalação. Entre com e-mail e senha.");
    }

    // O Google não respondeu (rede, fora do ar): não é culpa da pessoa
    public LoginComGoogleIndisponivelException(Throwable causa) {
        super("Não conseguimos falar com o Google agora. Tente de novo em instantes.", causa);
    }
}
