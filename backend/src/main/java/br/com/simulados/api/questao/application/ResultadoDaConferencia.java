package br.com.simulados.api.questao.application;

import java.util.List;

// Contadores da carga. `inalteradas` é o que prova a idempotência: rodar duas
// vezes seguidas tem que dar 0 em gabaritosCorrigidos e em questoesAnuladas.
public record ResultadoDaConferencia(
        int gabaritosCorrigidos,
        int questoesAnuladas,
        int inalteradas,
        // O gabarito no banco não é o que o arquivo esperava NEM o que o INEP diz:
        // alguém mexeu depois. Não se sobrescreve, e a linha é mostrada pra decisão humana
        List<String> divergentes,
        // Linha do CSV sem questão no banco — banco de dev que só importou alguns anos
        List<String> semQuestao
) {

    @Override
    public String toString() {
        return "%d gabaritos corrigidos, %d questões anuladas, %d inalteradas, %d divergentes (não tocadas), %d sem questão no banco"
                .formatted(gabaritosCorrigidos, questoesAnuladas, inalteradas, divergentes.size(), semQuestao.size());
    }
}
