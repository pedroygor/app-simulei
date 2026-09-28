package br.com.simulados.api.questao.domain;

// Identifica uma questão na fonte. Idioma faz parte da chave porque as
// questões 1-5 de linguagens repetem o mesmo número em inglês e espanhol.
public record ChaveNaturalQuestao(Integer numero, Idioma idioma) {
}
