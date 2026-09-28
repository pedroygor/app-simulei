package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.LinhaDaTaxonomia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A taxonomia em árvore: área → matéria → assunto, com quantas questões
 * válidas cada assunto tem. É o que a tela de montar prova usa pra mostrar
 * "Probabilidade (87)" antes de o professor escolher a quantidade.
 */
public record AreaDaTaxonomiaResponse(Area area, List<MateriaResponse> materias) {

    public record MateriaResponse(String slug, String nome, List<AssuntoResponse> assuntos) {
    }

    public record AssuntoResponse(String slug, String nome, long questoes) {
    }

    /**
     * Monta a árvore a partir das linhas achatadas da consulta.
     *
     * Não consulta nada: o banco já foi chamado UMA vez, e aqui só se
     * reorganiza o que veio. LinkedHashMap nos dois níveis preserva a ordem
     * da query (a do docs/taxonomia.md) — um HashMap devolveria as áreas numa
     * ordem diferente a cada execução.
     */
    public static List<AreaDaTaxonomiaResponse> montar(List<LinhaDaTaxonomia> linhas) {
        var porAreaEMateria = linhas.stream().collect(Collectors.groupingBy(
                LinhaDaTaxonomia::area, LinkedHashMap::new,
                Collectors.groupingBy(LinhaDaTaxonomia::materiaSlug, LinkedHashMap::new, Collectors.toList())));

        return porAreaEMateria.entrySet().stream()
                .map(area -> new AreaDaTaxonomiaResponse(area.getKey(), area.getValue().values().stream()
                        .map(AreaDaTaxonomiaResponse::materia)
                        .toList()))
                .toList();
    }

    private static MateriaResponse materia(List<LinhaDaTaxonomia> linhasDaMateria) {
        // Toda linha do grupo tem a mesma matéria; a primeira basta pro nome
        var primeira = linhasDaMateria.getFirst();
        var assuntos = linhasDaMateria.stream()
                .map(linha -> new AssuntoResponse(linha.assuntoSlug(), linha.assuntoNome(), linha.questoes()))
                .toList();
        return new MateriaResponse(primeira.materiaSlug(), primeira.materiaNome(), assuntos);
    }
}
