package br.com.simulados.api.desempenho.domain;

import br.com.simulados.api.simulado.domain.Placar;

import java.util.Comparator;

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

    /**
     * Com 5 alternativas, chutando se acerta 20%. Até 30% (uma folga pequena acima
     * disso) o aluno não está sabendo, está adivinhando — a tela diz "perto do chute".
     */
    public static final int LIMITE_DO_CHUTE_PERCENTUAL = 30;

    /**
     * Do pior pro melhor, comparando FRAÇÕES por multiplicação cruzada (a/b < c/d ⇔
     * a·d < c·b), como o Placar.PIOR_PRIMEIRO: com percentual inteiro, 1 de 3 e 33 de
     * 100 empatariam. No empate, quem tem MAIS respostas vem antes — é o erro mais
     * confirmado. Sem resposta vai pro fim (não há o que comparar).
     */
    public static final Comparator<Aproveitamento> PIOR_PRIMEIRO = Comparator
            .<Aproveitamento, Boolean>comparing(a -> a.respondidas == 0)
            .thenComparing((a1, a2) -> Long.compare(a1.acertos * a2.respondidas, a2.acertos * a1.respondidas))
            .thenComparing(Aproveitamento::respondidas, Comparator.reverseOrder());

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

    /**
     * Acerto em até 30%, com amostra suficiente. A conta é na fração exata
     * (acertos/respondidas ≤ 30/100), não no percentual arredondado pra baixo: 31%
     * arredondado não pode cair pra dentro. Sem amostra é sempre false: 1 de 4 é 25%,
     * mas com 4 respostas isso ainda é sorte.
     */
    public boolean pertoDoChute() {
        return amostraSuficiente() && acertos * 100 <= (long) LIMITE_DO_CHUTE_PERCENTUAL * respondidas;
    }

    public Aproveitamento somar(Aproveitamento outro) {
        return new Aproveitamento(respondidas + outro.respondidas, acertos + outro.acertos, emBranco + outro.emBranco);
    }
}
