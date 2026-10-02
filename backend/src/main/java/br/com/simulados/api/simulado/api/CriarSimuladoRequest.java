package br.com.simulados.api.simulado.api;

import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.simulado.application.PedidoDeSimulado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido de prova (Etapa 3a). O Bean Validation cobre o FORMATO de cada campo e de
 * cada item; as regras sobre a lista inteira ("nº de escolhas ≤ total", "sem
 * sobreposição", "há questões suficientes") dependem do banco e ficam no service.
 *
 * As anotações DENTRO do tipo genérico — List<@Valid @NotNull EscolhaDoSimulado>,
 * List<@NotNull @Min(2009) Integer> — validam cada elemento. Sem elas o Bean
 * Validation olharia só a lista, e `quantidade: -3` ou `anos: [1990]` passariam.
 */
public record CriarSimuladoRequest(
        @NotBlank @Size(max = 120) String titulo,
        @NotNull @Min(5) @Max(180) Integer total,
        // vazia ou ausente = todos os anos
        @Size(max = 20) List<@NotNull @Min(2009) @Max(2030) Integer> anos,
        // null = sem língua estrangeira ao escolher área ou matéria inteira
        Idioma idioma,
        @NotEmpty @Size(max = 30) List<@Valid @NotNull EscolhaDoSimulado> itens,
        // null = sem limite de tempo; 1 min a 6 h, o mesmo CHECK da V10
        @Min(60) @Max(21600) Integer tempoLimiteSeg
) {

    PedidoDeSimulado paraPedido() {
        return new PedidoDeSimulado(titulo, total, anos == null ? List.of() : anos, idioma,
                itens.stream().map(EscolhaDoSimulado::paraEscolha).toList(), tempoLimiteSeg);
    }
}
