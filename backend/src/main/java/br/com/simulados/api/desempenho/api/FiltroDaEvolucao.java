package br.com.simulados.api.desempenho.api;

import br.com.simulados.api.desempenho.application.EvolucaoDoDesempenho;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Query params de `GET /desempenho/evolucao`, montados pelo Spring a partir de
 * `?semanas=` e validados pelo @Valid do controller (mesmo desenho do
 * FiltroDaTaxonomia). Teto de 52: um ano é o máximo que faz sentido num gráfico
 * semanal, e sem teto `?semanas=100000` gastaria memória à toa.
 */
public record FiltroDaEvolucao(@Min(1) @Max(52) Integer semanas) {

    /** Sem o parâmetro, as últimas 4 semanas (decidido em 20/10/2026). */
    public int semanasOuPadrao() {
        return semanas == null ? EvolucaoDoDesempenho.SEMANAS_PADRAO : semanas;
    }
}
