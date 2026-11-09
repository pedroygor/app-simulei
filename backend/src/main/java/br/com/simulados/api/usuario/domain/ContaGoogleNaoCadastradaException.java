package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.RecursoNaoEncontradoException;

// Não é erro da pessoa: é o sinal pro front abrir "completar cadastro"
public class ContaGoogleNaoCadastradaException extends RecursoNaoEncontradoException {

    public ContaGoogleNaoCadastradaException() {
        super("Ainda não existe conta do Simulei ligada a esta conta Google.");
    }
}
