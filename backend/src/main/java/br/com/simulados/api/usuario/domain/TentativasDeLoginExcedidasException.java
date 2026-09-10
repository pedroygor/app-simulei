package br.com.simulados.api.usuario.domain;

import br.com.simulados.api.shared.exception.MuitasRequisicoesException;

import java.time.Duration;

/**
 * Conta temporariamente bloqueada por excesso de tentativas de login.
 *
 * É regra de negócio, não detalhe de HTTP: o service lança, e o
 * GlobalExceptionHandler traduz pra 429.
 */
public class TentativasDeLoginExcedidasException extends MuitasRequisicoesException {

    public TentativasDeLoginExcedidasException(Duration esperaRestante) {
        super("Muitas tentativas de login. Tente de novo em %d minuto(s)."
                .formatted(Math.max(1, esperaRestante.toMinutes())));
    }
}
