package br.com.simulados.api.simulado.domain;

/**
 * Como uma prova montada aparece no histórico. Não é o StatusDaTentativa: aqui
 * existe "não iniciada", que é a AUSÊNCIA de tentativa (montou e não começou).
 */
public enum SituacaoDaProva {
    NAO_INICIADA,
    EM_ANDAMENTO,
    ENTREGUE
}
