package br.com.simulados.api.simulado.api;

import br.com.simulados.api.simulado.application.ProvasEmAndamento.Abertas;
import br.com.simulados.api.simulado.domain.ProvaEmAndamento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * As provas abertas do painel. `agora` é a hora do servidor (mesma ideia do
 * ProvaResponse): o tempo restante é calculado com ela, não com o relógio do celular.
 */
public record ProvasEmAndamentoResponse(Instant agora, List<Aberta> provas) {

    /** `prazo` null = sem cronômetro. Sem acertos: a prova está aberta (revelaria o gabarito). */
    public record Aberta(UUID tentativaId, UUID simuladoId, String titulo, Instant iniciadoEm, Instant prazo,
                         long questoes, long respondidas) {

        static Aberta de(ProvaEmAndamento p) {
            return new Aberta(p.tentativaId(), p.simuladoId(), p.titulo(), p.iniciadoEm(), p.prazo(),
                    p.questoes(), p.respondidas());
        }
    }

    public static ProvasEmAndamentoResponse de(Abertas abertas) {
        return new ProvasEmAndamentoResponse(abertas.agora(), abertas.provas().stream().map(Aberta::de).toList());
    }
}
