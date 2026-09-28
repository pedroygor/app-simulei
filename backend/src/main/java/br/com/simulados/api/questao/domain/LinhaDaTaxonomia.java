package br.com.simulados.api.questao.domain;

/**
 * Uma linha "achatada" da taxonomia: um assunto com a matéria e a área dele,
 * mais quantas questões válidas tem. A consulta devolve as 118 linhas numa
 * query só; quem transforma isso em árvore é a camada da API.
 */
public record LinhaDaTaxonomia(
        Area area,
        String materiaSlug,
        String materiaNome,
        String assuntoSlug,
        String assuntoNome,
        long questoes
) {
}
