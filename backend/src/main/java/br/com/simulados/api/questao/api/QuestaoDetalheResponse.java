package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;

import java.util.List;

// Questão completa. O gabarito só vai pro professor (ver QuestaoController.porId);
// pro aluno ele sai null, e o aluno o vê no resultado da prova finalizada (3c).
public record QuestaoDetalheResponse(
        Long id,
        Integer ano,
        Integer numero,
        Area area,
        Idioma idioma,
        // {slug, nome}; null = questão ainda não classificada
        RotuloResponse materia,
        RotuloResponse assunto,
        // Habilidade oficial do INEP; null quando o INEP não publicou (2009 fora
        // de Matemática, item sem habilidade). A dificuldade TRI fica de fora de
        // propósito: a escala crua não diz nada a quem lê a questão
        HabilidadeResponse habilidade,
        String enunciado,
        String comando,
        String gabarito,
        List<AlternativaResponse> alternativas
) {

    // Mesma questão sem a resposta, pra quem não pode vê-la fora da prova
    public QuestaoDetalheResponse semGabarito() {
        return new QuestaoDetalheResponse(id, ano, numero, area, idioma, materia, assunto, habilidade,
                enunciado, comando, null, alternativas);
    }
}
