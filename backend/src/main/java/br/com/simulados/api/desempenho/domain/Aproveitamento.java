package br.com.simulados.api.desempenho.domain;

import br.com.simulados.api.simulado.domain.Placar;

/**
 * O que o aluno mostrou saber num recorte (tudo, uma área, um assunto), somando
 * as provas entregues.
 *
 * Por que não o {@link Placar} da 3c: o Placar divide pelo TOTAL de questões, que é
 * a regra da NOTA (no ENEM, em branco vale zero). Aqui a pergunta é outra — "o que
 * ele sabe deste assunto" — e a questão em branco não responde a ela: pode ser "não
 * sei", mas também "acabou o tempo" ou "pulei e esqueci". O tempo acabando castiga as
 * ÚLTIMAS questões da prova, e a prova é montada agrupada por escolha: dividir pelo
 * total jogaria a culpa do relógio no último assunto da lista (diagnóstico falso).
 * Então o percentual é sobre as RESPONDIDAS, e a em branco vira sinal à parte
 * ({@link #emBranco}), de comportamento e não de assunto.
 */
public record Aproveitamento(long respondidas, long acertos, long emBranco) {

    /**
     * Abaixo disto o percentual é sorte demais pra virar "ponto fraco": com 2
     * respostas, um chute certo é 50%. Conta RESPONDIDAS, pela mesma razão do
     * percentual.
     */
    public static final int AMOSTRA_MINIMA = 5;

    public static final Aproveitamento VAZIO = new Aproveitamento(0, 0, 0);

    public static Aproveitamento de(Placar placar) {
        return new Aproveitamento(placar.respondidas(), placar.acertos(), placar.emBranco());
    }

    /** Acerto em % sobre as respondidas, pra baixo. NULL sem resposta: 0% diria "errou tudo". */
    public Integer percentual() {
        return respondidas == 0 ? null : (int) (acertos * 100 / respondidas);
    }

    public boolean amostraSuficiente() {
        return respondidas >= AMOSTRA_MINIMA;
    }

    public Aproveitamento somar(Aproveitamento outro) {
        return new Aproveitamento(respondidas + outro.respondidas, acertos + outro.acertos, emBranco + outro.emBranco);
    }
}
