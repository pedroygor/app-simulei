package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.shared.exception.NegocioException;

/**
 * Responder ou finalizar de novo uma prova já entregue. 422 e não 409: não é
 * corrida entre duas escritas, é a regra "prova entregue não muda" — senão o
 * aluno veria o gabarito (3c) e voltaria pra corrigir as respostas.
 */
public class TentativaJaFinalizadaException extends NegocioException {

    public TentativaJaFinalizadaException() {
        super("Esta prova já foi finalizada e não aceita mais respostas");
    }
}
