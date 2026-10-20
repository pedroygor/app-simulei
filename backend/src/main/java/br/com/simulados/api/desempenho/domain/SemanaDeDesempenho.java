package br.com.simulados.api.desempenho.domain;

import br.com.simulados.api.questao.domain.Area;

import java.time.LocalDate;
import java.util.Map;

/**
 * Uma semana da evolução. Semana sem prova entregue vem com `geral` VAZIO
 * (percentual null) e sem áreas: é um BURACO no gráfico, nunca 0% — zero diria
 * "errou tudo" numa semana em que o aluno só não estudou.
 *
 * @param inicio  a segunda-feira, no fuso de Brasília
 * @param porArea na ordem do enum, só as áreas com questão naquela semana
 */
public record SemanaDeDesempenho(LocalDate inicio, Aproveitamento geral, Map<Area, Aproveitamento> porArea) {
}
