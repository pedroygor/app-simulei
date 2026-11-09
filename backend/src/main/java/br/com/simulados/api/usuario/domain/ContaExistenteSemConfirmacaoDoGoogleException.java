package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.ConflitoException;

// Há conta com o e-mail, mas o Google não confirmou que a pessoa é dona dele.
// Ligar mesmo assim deixaria quem criou uma conta Google com o e-mail de outra
// pessoa entrar na conta dela
public class ContaExistenteSemConfirmacaoDoGoogleException extends ConflitoException {

    public ContaExistenteSemConfirmacaoDoGoogleException() {
        super("Já existe uma conta com este e-mail. Entre com e-mail e senha.");
    }
}
