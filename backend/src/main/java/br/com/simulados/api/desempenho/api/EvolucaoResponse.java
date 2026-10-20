package br.com.simulados.api.desempenho.api;

import br.com.simulados.api.desempenho.api.DesempenhoResponse.AproveitamentoResponse;
import br.com.simulados.api.desempenho.api.DesempenhoResponse.PorArea;
import br.com.simulados.api.desempenho.domain.Aproveitamento;
import br.com.simulados.api.desempenho.domain.SemanaDeDesempenho;

import java.time.LocalDate;
import java.util.List;

/**
 * A evolução semanal. Toda semana da janela vem, em ordem, inclusive as vazias
 * (`geral.percentual` null): a tela desenha um buraco, nunca 0%.
 */
public record EvolucaoResponse(int amostraMinima, List<Semana> semanas) {

    /** `inicio` = a segunda-feira (fuso de Brasília), em ISO (2026-10-13). */
    public record Semana(LocalDate inicio, AproveitamentoResponse geral, List<PorArea> porArea) {
    }

    public static EvolucaoResponse de(List<SemanaDeDesempenho> semanas) {
        return new EvolucaoResponse(Aproveitamento.AMOSTRA_MINIMA, semanas.stream()
                .map(s -> new Semana(s.inicio(), AproveitamentoResponse.de(s.geral()), s.porArea().entrySet().stream()
                        .map(e -> new PorArea(e.getKey(), e.getKey().getNome(), AproveitamentoResponse.de(e.getValue())))
                        .toList()))
                .toList());
    }
}
