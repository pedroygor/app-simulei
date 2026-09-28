package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;

import java.util.List;

// Questão completa. O gabarito vai junto: hoje quem consome é o professor
// montando simulado. Quando existir a tela de prova do aluno (Etapa 3), a
// resposta da tentativa vai usar um DTO próprio, SEM gabarito.
public record QuestaoDetalheResponse(
        Long id,
        Integer ano,
        Integer numero,
        Area area,
        Idioma idioma,
        // {slug, nome}; null = questão ainda não classificada
        RotuloResponse materia,
        RotuloResponse assunto,
        String enunciado,
        String comando,
        String gabarito,
        List<AlternativaResponse> alternativas
) {
}
