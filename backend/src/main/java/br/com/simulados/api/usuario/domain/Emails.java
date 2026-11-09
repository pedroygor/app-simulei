package br.com.simulados.api.usuario.domain;

import java.util.Locale;

/**
 * A forma única de um e-mail no Simulei: sem espaço nas pontas e em minúsculas.
 * Todo e-mail que entra (cadastro, login, Google) passa por aqui antes de tocar o
 * banco; o CHECK da V14 recusa o que escapar.
 */
public final class Emails {

    private Emails() {
    }

    public static String normalizar(String email) {
        // Locale.ROOT: o toLowerCase() sem argumento usa o idioma da JVM, e numa
        // máquina em turco o "I" viraria "ı" (sem pingo), um e-mail diferente
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
