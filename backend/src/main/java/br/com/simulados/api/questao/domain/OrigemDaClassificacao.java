package br.com.simulados.api.questao.domain;

/**
 * Quem deu o assunto à questão. Importa porque a carga da classificação por
 * IA NUNCA sobrescreve uma revisão humana: rodar o classificador de novo não
 * pode desfazer o trabalho de quem corrigiu à mão.
 */
public enum OrigemDaClassificacao {
    IA,
    REVISAO_HUMANA
}
