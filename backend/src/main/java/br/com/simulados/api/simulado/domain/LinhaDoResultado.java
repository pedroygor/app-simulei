package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.questao.domain.Area;

/**
 * Uma linha do GROUP BY do resultado: o placar de um assunto numa tentativa.
 * Matéria e assunto são NULL pra questão ainda não classificada (LEFT JOIN).
 */
public record LinhaDoResultado(
        Area area,
        String materiaSlug,
        String materiaNome,
        String assuntoSlug,
        String assuntoNome,
        long questoes,
        long respondidas,
        long acertos
) {

    public Placar placar() {
        return new Placar(questoes, respondidas, acertos);
    }
}
