package br.com.simulados.api.questao.application;

import java.util.List;

// A 2ª rodada seguida com o mesmo arquivo tem que dar 0 marcadas e 0 liberadas
public record ResultadoDasIncompletas(
        int marcadas,
        // Estava marcada e saiu do arquivo: volta a poder ser mostrada e sorteada
        int liberadas,
        int inalteradas,
        List<String> semQuestao
) {

    @Override
    public String toString() {
        return "%d marcadas, %d liberadas, %d inalteradas, %d sem questão no banco"
                .formatted(marcadas, liberadas, inalteradas, semQuestao.size());
    }
}
