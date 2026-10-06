package br.com.simulados.api.simulado.domain;

import java.util.Comparator;

/**
 * Quantas questões, quantas respondidas e quantas certas — de uma prova inteira,
 * de uma área ou de um assunto. Em branco = questões − respondidas.
 */
public record Placar(long questoes, long respondidas, long acertos) {

    public static final Placar VAZIO = new Placar(0, 0, 0);

    public long emBranco() {
        return questoes - respondidas;
    }

    /**
     * Acerto em %, arredondado pra baixo. NULL quando não há questão: 0% diria
     * "errou tudo", e não houve nada pra errar (seção 8.4, divisão por zero).
     */
    public Integer percentual() {
        return questoes == 0 ? null : (int) (acertos * 100 / questoes);
    }

    public Placar somar(Placar outro) {
        return new Placar(questoes + outro.questoes, respondidas + outro.respondidas, acertos + outro.acertos);
    }

    /**
     * Do pior aproveitamento pro melhor, comparando FRAÇÕES por multiplicação
     * cruzada (a/b < c/d ⇔ a·d < c·b): sem divisão nem arredondamento, 1 de 3 e
     * 33 de 100 não empatam. Placar sem questão vai pro fim.
     */
    public static final Comparator<Placar> PIOR_PRIMEIRO = (p1, p2) -> {
        if (p1.questoes == 0 || p2.questoes == 0) {
            return Boolean.compare(p1.questoes == 0, p2.questoes == 0);
        }
        return Long.compare(p1.acertos * p2.questoes, p2.acertos * p1.questoes);
    };
}
