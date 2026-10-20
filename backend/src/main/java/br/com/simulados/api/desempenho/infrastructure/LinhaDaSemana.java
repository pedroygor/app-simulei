package br.com.simulados.api.desempenho.infrastructure;

import java.time.LocalDate;

/**
 * Uma linha do GROUP BY da evolução: uma área numa semana. Projeção por interface:
 * o Spring Data implementa estes getters a partir das colunas da query nativa (pelo
 * NOME do alias), sem entidade nem conversão manual.
 */
public interface LinhaDaSemana {

    /** Segunda-feira da semana, no fuso de Brasília. */
    LocalDate getSemana();

    /** O nome do enum Area (a query nativa devolve texto). */
    String getArea();

    long getQuestoes();

    long getRespondidas();

    long getAcertos();
}
