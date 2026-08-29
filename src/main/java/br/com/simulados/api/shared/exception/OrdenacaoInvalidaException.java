package br.com.simulados.api.shared.exception;

import java.util.Collection;

public class OrdenacaoInvalidaException extends RequisicaoInvalidaException {

    public OrdenacaoInvalidaException(Collection<String> invalidos, Collection<String> permitidos) {
        super("Campo de ordenação inválido: %s. Use um de: %s"
                .formatted(String.join(", ", invalidos), String.join(", ", permitidos)));
    }
}
