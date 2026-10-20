package br.com.simulados.api.simulado.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma prova aberta, pro "Continuar" do painel (4d). Questões e respondidas contam só
 * as `disponivel` (a mesma régua da prova e da correção).
 *
 * @param prazo null = sem cronômetro
 */
public record ProvaEmAndamento(
        UUID tentativaId,
        UUID simuladoId,
        String titulo,
        Instant iniciadoEm,
        Instant prazo,
        long questoes,
        long respondidas
) {
}
