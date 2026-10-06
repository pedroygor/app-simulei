package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.questao.domain.Area;

// Quantas questões do simulado são de cada assunto (projeção do GROUP BY)
public record LinhaDaComposicao(
        Area area,
        String materiaSlug,
        String materiaNome,
        String assuntoSlug,
        String assuntoNome,
        long questoes
) {
}
