package br.com.simulados.api.simulado.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A alternativa marcada, pelo ID e não pela letra (seção 6): a letra deixa de
 * identificar a opção quando houver embaralhamento. Se ela é desta questão é
 * regra de negócio (service, 422); aqui só o formato.
 */
public record ResponderRequest(
        @NotNull @Positive Long alternativaId
) {
}
