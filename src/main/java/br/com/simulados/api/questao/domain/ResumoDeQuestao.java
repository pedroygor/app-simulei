package br.com.simulados.api.questao.domain;

/**
 * Projeção de leitura pra listagem. Existe pra NÃO carregar a entidade inteira:
 * o enunciado de uma questão do ENEM chega a milhares de caracteres, e trazer
 * 20 deles por página só pra mostrar uma prévia é desperdício de rede e memória.
 */
public record ResumoDeQuestao(
        Long id,
        Integer ano,
        Integer numero,
        Area area,
        Idioma idioma,
        String materia,
        String assunto,
        String previa
) {
}
