package br.com.simulados.api.simulado.api;

import br.com.simulados.api.simulado.api.ResultadoResponse.PlacarResponse;
import br.com.simulados.api.simulado.domain.ItemDoHistorico;
import br.com.simulados.api.simulado.domain.SituacaoDaProva;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma prova em "minhas provas". `placar` é NULL enquanto a prova não foi entregue:
 * com ela aberta, acertos vazariam o gabarito (ver ItemDoHistorico). Antes da
 * entrega, o que dá pra mostrar é `respondidas`.
 */
public record ProvaDoHistoricoResponse(
        UUID simuladoId,
        String titulo,
        Instant criadoEm,
        SituacaoDaProva situacao,
        // null = não iniciada
        UUID tentativaId,
        Instant iniciadoEm,
        Instant finalizadoEm,
        long questoes,
        long respondidas,
        PlacarResponse placar
) {

    public static ProvaDoHistoricoResponse de(ItemDoHistorico item) {
        var placar = item.placarSeEntregue();
        return new ProvaDoHistoricoResponse(item.simuladoId(), item.titulo(), item.criadoEm(), item.situacao(),
                item.tentativaId(), item.iniciadoEm(), item.finalizadoEm(), item.questoes(), item.respondidas(),
                placar == null ? null : PlacarResponse.de(placar));
    }
}
