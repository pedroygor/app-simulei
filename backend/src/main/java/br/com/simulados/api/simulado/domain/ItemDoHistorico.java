package br.com.simulados.api.simulado.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha do histórico: o simulado e, se existir, a tentativa do usuário nele,
 * com o placar já agregado pelo banco (projeção do GROUP BY).
 * Campos da tentativa são NULL quando a prova não foi iniciada.
 */
public record ItemDoHistorico(
        UUID simuladoId,
        String titulo,
        Instant criadoEm,
        UUID tentativaId,
        StatusDaTentativa status,
        Instant iniciadoEm,
        Instant finalizadoEm,
        long questoes,
        long respondidas,
        long acertos
) {

    public SituacaoDaProva situacao() {
        if (tentativaId == null) return SituacaoDaProva.NAO_INICIADA;
        return status == StatusDaTentativa.FINALIZADA ? SituacaoDaProva.ENTREGUE : SituacaoDaProva.EM_ANDAMENTO;
    }

    /**
     * O placar só existe pra prova ENTREGUE. Com a prova aberta, os acertos
     * vazariam o gabarito: responder, olhar o histórico e ver o número subir diz
     * se a resposta estava certa. Antes da entrega, só "respondidas" é seguro.
     */
    public Placar placarSeEntregue() {
        return situacao() == SituacaoDaProva.ENTREGUE ? new Placar(questoes, respondidas, acertos) : null;
    }
}
