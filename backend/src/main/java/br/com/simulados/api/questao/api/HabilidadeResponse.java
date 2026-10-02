package br.com.simulados.api.questao.api;

/**
 * Habilidade oficial da Matriz do ENEM no detalhe da questão: "H17" com o texto,
 * e a competência de área a que pertence. O texto vai junto porque o código
 * sozinho não diz nada (e se repete entre áreas: a H17 de Matemática não é a de
 * Natureza).
 */
public record HabilidadeResponse(Short codigo, String descricao, CompetenciaResponse competencia) {

    public record CompetenciaResponse(Short numero, String descricao) {
    }
}
