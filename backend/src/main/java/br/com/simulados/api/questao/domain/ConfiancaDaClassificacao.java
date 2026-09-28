package br.com.simulados.api.questao.domain;

/**
 * Confiança declarada pelo classificador. BAIXA sinaliza revisão: a regra 6
 * do docs/taxonomia.md manda marcar dúvida em vez de chutar.
 */
public enum ConfiancaDaClassificacao {
    ALTA,
    MEDIA,
    BAIXA
}
