package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.ConflitoException;

// A conta com este e-mail já foi ligada a uma conta Google diferente (outro
// "sub"). Trocar em silêncio entregaria a conta a quem chegou por último
public class ContaLigadaAOutroGoogleException extends ConflitoException {

    public ContaLigadaAOutroGoogleException() {
        super("Esta conta do Simulei já está ligada a outra conta Google.");
    }
}
