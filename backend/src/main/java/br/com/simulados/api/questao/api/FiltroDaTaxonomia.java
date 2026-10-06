package br.com.simulados.api.questao.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Anos em que as questões são contadas (?anos=2022&anos=2023). Ausente = todos.
 * Existe pra tela de montar prova (3a) mostrar quantas questões há em cada assunto
 * NOS ANOS ESCOLHIDOS — a contagem do banco inteiro prometeria questões que o
 * sorteio não pode usar.
 *
 * Mesmos limites do CriarSimuladoRequest, pra tela e sorteio aceitarem os mesmos
 * anos. A anotação dentro do tipo genérico valida cada ano, não só a lista.
 */
public record FiltroDaTaxonomia(
        @Size(max = 20) List<@NotNull @Min(2009) @Max(2030) Integer> anos
) {

    List<Integer> anosOuTodos() {
        return anos == null ? List.of() : anos.stream().distinct().toList();
    }
}
