package br.com.simulados.api.simulado.api;

import br.com.simulados.api.simulado.application.PedidoDeSimulado;
import br.com.simulados.api.simulado.domain.TipoDeEscolha;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Uma escolha do pedido: uma área inteira (valor = MATEMATICA), uma matéria ou um
 * assunto (valor = slug). O @Pattern cobre os dois formatos e barra qualquer outra
 * coisa antes de chegar ao service, que confere se o valor existe.
 */
public record EscolhaDoSimulado(
        @NotNull TipoDeEscolha tipo,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,60}") String valor,
        // null = o sistema distribui
        @Min(1) @Max(180) Integer quantidade
) {

    PedidoDeSimulado.Escolha paraEscolha() {
        return new PedidoDeSimulado.Escolha(tipo, valor, quantidade);
    }
}
