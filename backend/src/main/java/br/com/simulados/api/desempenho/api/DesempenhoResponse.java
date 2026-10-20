package br.com.simulados.api.desempenho.api;

import br.com.simulados.api.desempenho.application.ConsultaDeDesempenho.Desempenho;
import br.com.simulados.api.desempenho.domain.Aproveitamento;
import br.com.simulados.api.desempenho.domain.AproveitamentoNoAssunto;
import br.com.simulados.api.questao.api.RotuloResponse;
import br.com.simulados.api.questao.domain.Area;

import java.util.List;

public record DesempenhoResponse(
        long provas,
        // A regra vai junto pra tela dizer "responda mais N" sem repetir a constante
        int amostraMinima,
        AproveitamentoResponse geral,
        List<PorArea> porArea,
        List<PorAssunto> porAssunto,
        // Até 3 assuntos medidos, do menor acerto pro maior (4b)
        List<PorAssunto> pontosFracos
) {

    /**
     * percentual = acertos / RESPONDIDAS (null sem resposta). emBranco é sinal à
     * parte: não entra no percentual (ver Aproveitamento).
     */
    public record AproveitamentoResponse(long respondidas, long acertos, long emBranco,
                                         Integer percentual, boolean amostraSuficiente,
                                         // Acerto até 30% com amostra: adivinhando, não sabendo
                                         boolean pertoDoChute) {

        static AproveitamentoResponse de(Aproveitamento a) {
            return new AproveitamentoResponse(a.respondidas(), a.acertos(), a.emBranco(),
                    a.percentual(), a.amostraSuficiente(), a.pertoDoChute());
        }
    }

    public record PorArea(Area area, String nome, AproveitamentoResponse aproveitamento) {
    }

    public record PorAssunto(Area area, RotuloResponse materia, RotuloResponse assunto,
                             AproveitamentoResponse aproveitamento) {

        static PorAssunto de(AproveitamentoNoAssunto a) {
            return new PorAssunto(a.area(), new RotuloResponse(a.materiaSlug(), a.materiaNome()),
                    new RotuloResponse(a.assuntoSlug(), a.assuntoNome()), AproveitamentoResponse.de(a.aproveitamento()));
        }
    }

    public static DesempenhoResponse de(Desempenho desempenho) {
        return new DesempenhoResponse(
                desempenho.provas(),
                Aproveitamento.AMOSTRA_MINIMA,
                AproveitamentoResponse.de(desempenho.geral()),
                desempenho.porArea().entrySet().stream()
                        .map(e -> new PorArea(e.getKey(), e.getKey().getNome(), AproveitamentoResponse.de(e.getValue())))
                        .toList(),
                desempenho.porAssunto().stream().map(PorAssunto::de).toList(),
                desempenho.pontosFracos().stream().map(PorAssunto::de).toList());
    }
}
