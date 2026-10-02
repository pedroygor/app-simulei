package br.com.simulados.api.questao.application;

import java.util.List;

// `inalteradas` prova a idempotência: a 2ª rodada seguida tem que dar 0 atualizadas
public record ResultadoDasHabilidades(
        int atualizadas,
        int inalteradas,
        // Linha sem questão no banco — banco de dev que só importou alguns anos
        List<String> semQuestao
) {

    @Override
    public String toString() {
        return "%d atualizadas, %d inalteradas, %d sem questão no banco"
                .formatted(atualizadas, inalteradas, semQuestao.size());
    }
}
