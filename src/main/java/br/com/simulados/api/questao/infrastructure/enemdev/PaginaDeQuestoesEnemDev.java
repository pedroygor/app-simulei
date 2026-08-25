package br.com.simulados.api.questao.infrastructure.enemdev;

import java.util.List;

public record PaginaDeQuestoesEnemDev(
        Metadados metadata,
        List<QuestaoEnemDev> questions
) {

    public record Metadados(
            int limit,
            int offset,
            int total,
            boolean hasMore
    ) {
    }
}
