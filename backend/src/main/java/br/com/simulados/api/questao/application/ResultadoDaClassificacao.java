package br.com.simulados.api.questao.application;

import java.util.List;

// Contadores da carga. `inalteradas` é o que prova a idempotência: rodar a
// carga duas vezes seguidas tem que dar 0 atualizadas na segunda.
public record ResultadoDaClassificacao(
        int atualizadas,
        int inalteradas,
        // Questões com REVISAO_HUMANA: a IA nunca desfaz correção feita à mão
        int preservadasPorRevisaoHumana,
        // Linhas do CSV sem questão no banco — normal num banco de dev que só
        // importou alguns anos, por isso é contado e não recusado
        List<String> semQuestao
) {

    @Override
    public String toString() {
        return "%d atualizadas, %d inalteradas, %d preservadas por revisão humana, %d sem questão no banco"
                .formatted(atualizadas, inalteradas, preservadasPorRevisaoHumana, semQuestao.size());
    }
}
