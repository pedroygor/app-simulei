package br.com.simulados.api.simulado.application;

import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.simulado.domain.TipoDeEscolha;

import java.util.List;

/**
 * O que o service precisa pra montar uma prova, já sem nada de HTTP: o controller
 * converte o request validado nisto. Assim o service não depende do formato da API.
 *
 * @param anos   lista de anos; vazia = todos
 * @param idioma língua estrangeira que entra ao expandir área/matéria; null = nenhuma
 */
public record PedidoDeSimulado(
        String titulo,
        int total,
        List<Integer> anos,
        Idioma idioma,
        List<Escolha> escolhas,
        Integer tempoLimiteSeg
) {

    /** @param quantidade null = o sistema distribui */
    public record Escolha(TipoDeEscolha tipo, String valor, Integer quantidade) {
    }
}
