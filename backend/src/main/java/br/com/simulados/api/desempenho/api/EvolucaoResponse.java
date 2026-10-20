package br.com.simulados.api.desempenho.api;

import br.com.simulados.api.desempenho.api.DesempenhoResponse.AproveitamentoResponse;
import br.com.simulados.api.desempenho.api.DesempenhoResponse.PorArea;
import br.com.simulados.api.desempenho.domain.Aproveitamento;
import br.com.simulados.api.desempenho.domain.SemanaDeDesempenho;
import br.com.simulados.api.questao.api.RotuloResponse;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;

import java.time.LocalDate;
import java.util.List;

/**
 * A evolução semanal. Toda semana da janela vem, em ordem, inclusive as vazias
 * (`geral.percentual` null): a tela desenha um buraco, nunca 0%.
 */
public record EvolucaoResponse(
        int amostraMinima,
        // Só com ?assunto=: qual é, pra a tela não precisar de outra consulta
        AssuntoDaEvolucao assunto,
        List<Semana> semanas
) {

    public record AssuntoDaEvolucao(Area area, RotuloResponse materia, RotuloResponse assunto) {

        static AssuntoDaEvolucao de(Assunto a) {
            return new AssuntoDaEvolucao(a.getArea(), new RotuloResponse(a.getMateria().getSlug(), a.getMateria().getNome()),
                    new RotuloResponse(a.getSlug(), a.getNome()));
        }
    }

    /** `inicio` = a segunda-feira (fuso de Brasília), em ISO (2026-10-13). */
    public record Semana(LocalDate inicio, AproveitamentoResponse geral, List<PorArea> porArea) {
    }

    public static EvolucaoResponse de(List<SemanaDeDesempenho> semanas) {
        return de(null, semanas);
    }

    public static EvolucaoResponse de(Assunto assunto, List<SemanaDeDesempenho> semanas) {
        return new EvolucaoResponse(Aproveitamento.AMOSTRA_MINIMA,
                assunto == null ? null : AssuntoDaEvolucao.de(assunto), semanas.stream()
                .map(s -> new Semana(s.inicio(), AproveitamentoResponse.de(s.geral()), s.porArea().entrySet().stream()
                        .map(e -> new PorArea(e.getKey(), e.getKey().getNome(), AproveitamentoResponse.de(e.getValue())))
                        .toList()))
                .toList());
    }
}
