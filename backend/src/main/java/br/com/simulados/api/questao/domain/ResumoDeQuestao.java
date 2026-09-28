package br.com.simulados.api.questao.domain;

/**
 * Projeção de leitura pra listagem. Existe pra NÃO carregar a entidade inteira:
 * o enunciado de uma questão do ENEM chega a milhares de caracteres, e trazer
 * 20 deles por página só pra mostrar uma prévia é desperdício de rede e memória.
 *
 * Matéria e assunto vêm achatados (slug e nome separados) porque o construtor
 * de uma consulta JPQL só recebe valores simples — quem os agrupa em
 * {slug, nome} é o mapper da API. Todos nulos = questão ainda não classificada.
 */
public record ResumoDeQuestao(
        Long id,
        Integer ano,
        Integer numero,
        Area area,
        Idioma idioma,
        String materiaSlug,
        String materiaNome,
        String assuntoSlug,
        String assuntoNome,
        String previa
) {
}
