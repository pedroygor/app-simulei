package br.com.simulados.api.questao.infrastructure.enemdev;

public record AlternativaEnemDev(
        String letter,
        String text,
        String file,
        boolean isCorrect
) {
}
