package br.com.simulados.api.questao.infrastructure.enemdev;

import java.util.List;

public record QuestaoEnemDev(
        int index,
        int year,
        String discipline,
        String language,
        String context,
        String alternativesIntroduction,
        String correctAlternative,
        List<String> files,
        List<AlternativaEnemDev> alternatives
) {
}
