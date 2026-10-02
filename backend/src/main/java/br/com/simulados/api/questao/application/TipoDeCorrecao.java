package br.com.simulados.api.questao.application;

/**
 * O que o INEP corrige na questão. São coisas diferentes e não se misturam:
 * GABARITO troca a letra; ANULADA tira a questão de circulação. `IN_ITEM_ABAN`
 * do INEP NÃO é ANULADA — a maioria é abandono estatístico da TRI, questão que
 * valeu. Só o gabarito `X` é anulação (ver docs/conferencia-inep.md).
 */
public enum TipoDeCorrecao {
    GABARITO,
    ANULADA
}
