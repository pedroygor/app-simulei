package br.com.simulados.api.simulado.api;

import br.com.simulados.api.simulado.domain.StatusDaTentativa;
import br.com.simulados.api.simulado.domain.Tentativa;

import java.time.Instant;
import java.util.UUID;

// O estado da tentativa, sem a prova: resposta do iniciar e do finalizar
public record TentativaResponse(
        UUID id,
        UUID simuladoId,
        StatusDaTentativa status,
        Instant iniciadoEm,
        Instant finalizadoEm
) {

    public static TentativaResponse de(Tentativa tentativa) {
        return new TentativaResponse(tentativa.getId(), tentativa.getSimuladoId(), tentativa.getStatus(),
                tentativa.getIniciadoEm(), tentativa.getFinalizadoEm());
    }
}
