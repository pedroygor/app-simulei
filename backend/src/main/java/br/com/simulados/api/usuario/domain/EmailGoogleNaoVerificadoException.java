package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.NegocioException;

// Cadastro novo pelo Google com e-mail que o Google não confirmou: criaria uma
// conta presa a um e-mail que talvez seja de outra pessoa, bloqueando o dono
// de se cadastrar depois
public class EmailGoogleNaoVerificadoException extends NegocioException {

    public EmailGoogleNaoVerificadoException() {
        super("O Google ainda não confirmou o seu e-mail. Confirme-o no Google ou crie a conta com e-mail e senha.");
    }
}
