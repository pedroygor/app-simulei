package br.com.simulados.api.questao.api;

/**
 * Matéria ou assunto numa resposta da API: o slug (identificador permanente,
 * usado em filtro e URL) e o nome (pra exibir). Mandar só o nome obrigaria o
 * frontend a "adivinhar" o slug pra montar o filtro a partir de um clique.
 */
public record RotuloResponse(String slug, String nome) {
}
