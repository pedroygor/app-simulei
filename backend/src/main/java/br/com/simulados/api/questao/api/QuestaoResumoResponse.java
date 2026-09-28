package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;

/**
 * Item da listagem. NÃO traz alternativas de propósito: montar a lista com as
 * alternativas de cada questão dispararia uma query por item (N+1). Quem quer a
 * questão inteira chama GET /questoes/{id}.
 */
public record QuestaoResumoResponse(
        Long id,
        Integer ano,
        Integer numero,
        Area area,
        Idioma idioma,
        // {slug, nome}; null = questão ainda não classificada
        RotuloResponse materia,
        RotuloResponse assunto,
        // Prévia pra tela de escolha de questões — o texto completo pode ter
        // milhares de caracteres e não cabe numa lista
        String previa
) {
}
