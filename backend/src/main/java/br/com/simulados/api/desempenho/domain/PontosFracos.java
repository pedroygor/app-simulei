package br.com.simulados.api.desempenho.domain;

import java.util.Comparator;
import java.util.List;

/**
 * Os assuntos com menor acerto: por onde o aluno começa a treinar (4b).
 *
 * Só entra assunto MEDIDO (amostra mínima): com 2 respostas, um chute errado faria
 * de qualquer assunto o "pior". No máximo 3: mais que isso vira lista e dilui o
 * "por onde começar". Não há corte por percentual — quem acerta 85% em tudo também
 * vê os seus 3 menores; a tela fala em "menor acerto", não em "fraco".
 */
public final class PontosFracos {

    public static final int MAXIMO = 3;

    private PontosFracos() {
    }

    /** Sort estável: no empate total, fica a ordem da taxonomia que veio do banco. */
    public static List<AproveitamentoNoAssunto> de(List<AproveitamentoNoAssunto> assuntos) {
        return assuntos.stream()
                .filter(a -> a.aproveitamento().amostraSuficiente())
                .sorted(Comparator.comparing(AproveitamentoNoAssunto::aproveitamento, Aproveitamento.PIOR_PRIMEIRO))
                .limit(MAXIMO)
                .toList();
    }
}
