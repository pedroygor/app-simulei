package br.com.simulados.api.desempenho.api;

import br.com.simulados.api.desempenho.application.EvolucaoDoDesempenho;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static br.com.simulados.api.questao.api.BuscarQuestoesFiltro.SLUG;
import static br.com.simulados.api.questao.api.BuscarQuestoesFiltro.SLUG_INVALIDO;

/**
 * Query params de `GET /desempenho/evolucao`, montados pelo Spring a partir de
 * `?semanas=` e `?assunto=` e validados pelo @Valid do controller (mesmo desenho do
 * FiltroDaTaxonomia). Teto de 52: um ano é o máximo que faz sentido num gráfico
 * semanal, e sem teto `?semanas=100000` gastaria memória à toa.
 */
public record FiltroDaEvolucao(
        @Min(1) @Max(52) Integer semanas,
        // Mesma regra de slug da busca de questões: fora do formato é 400, antes do banco
        @Size(max = 60) @Pattern(regexp = SLUG, message = SLUG_INVALIDO) String assunto
) {

    /** Sem o parâmetro, as últimas 4 semanas (decidido em 20/10/2026). */
    public int semanasOuPadrao() {
        return semanas == null ? EvolucaoDoDesempenho.SEMANAS_PADRAO : semanas;
    }
}
