package br.com.simulados.api.simulado.application;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Decide QUANTAS questões cada escolha recebe, dado o total pedido e quantas
 * questões existem em cada escolha (nos anos pedidos). Só aritmética: quem conta
 * as disponíveis é o banco, numa query só, antes daqui.
 *
 * Regras (Etapa 3a do CLAUDE.md):
 * 1. Toda escolha recebe pelo menos 1 questão — escolher um assunto e ele não cair
 *    na prova seria mentir pro usuário.
 * 2. Escolha com quantidade fixa recebe exatamente aquilo.
 * 3. O resto do total se divide POR IGUAL entre as escolhas sem quantidade,
 *    respeitando o que cada uma tem: quem tem pouco recebe o que tem, e o que
 *    sobra vai pras outras. A sobra que não divide exato vai pra escolhas
 *    sorteadas, pra não favorecer sempre a primeira da lista.
 *
 * Classe pura (sem Spring, sem banco): o sorteio da sobra recebe o gerador de
 * números de fora, e o teste passa um gerador com semente fixa.
 */
public class DistribuidorDeQuestoes {

    /**
     * @param rotulo      nome da escolha pra mensagem de erro ("Probabilidade")
     * @param fixa        quantidade pedida; null = o sistema decide
     * @param disponiveis quantas questões existem na escolha, nos anos pedidos
     */
    public record Cota(String rotulo, Integer fixa, long disponiveis) {
    }

    private final RandomGenerator aleatorio;

    public DistribuidorDeQuestoes(RandomGenerator aleatorio) {
        this.aleatorio = aleatorio;
    }

    /** Quantas questões cada escolha recebe, na mesma ordem das cotas. */
    public List<Integer> distribuir(int total, List<Cota> cotas) {
        if (cotas.size() > total) {
            throw new PedidoDeSimuladoInvalidoException(
                    "%d escolhas não cabem numa prova de %d questões: cada escolha precisa de pelo menos 1"
                            .formatted(cotas.size(), total));
        }

        var faltas = new ArrayList<String>();
        for (var cota : cotas) {
            if (cota.disponiveis() == 0) {
                faltas.add("%s não tem questões nos anos escolhidos".formatted(cota.rotulo()));
            } else if (cota.fixa() != null && cota.fixa() > cota.disponiveis()) {
                faltas.add("%s: pediu %d, há %d".formatted(cota.rotulo(), cota.fixa(), cota.disponiveis()));
            }
        }
        if (!faltas.isEmpty()) {
            throw new QuestoesInsuficientesException(faltas);
        }

        var quantidades = new int[cotas.size()];
        var livres = new ArrayList<Integer>();
        int restante = total;
        for (int i = 0; i < cotas.size(); i++) {
            var fixa = cotas.get(i).fixa();
            if (fixa != null) {
                quantidades[i] = fixa;
                restante -= fixa;
            } else {
                livres.add(i);
            }
        }

        // As fixas consumiram o total e não sobrou 1 pra cada escolha livre
        if (restante < livres.size()) {
            throw new PedidoDeSimuladoInvalidoException(
                    "as quantidades fixas somam %d e não sobra pelo menos 1 questão pra cada uma das outras %d escolhas (total %d)"
                            .formatted(total - restante, livres.size(), total));
        }
        // Sem escolha livre, as fixas têm que fechar o total exato
        if (livres.isEmpty() && restante != 0) {
            throw new PedidoDeSimuladoInvalidoException(
                    "as quantidades fixas somam %d, mas o total da prova é %d".formatted(total - restante, total));
        }

        distribuirPorIgual(total, restante, livres, quantidades, cotas);

        var resultado = new ArrayList<Integer>(cotas.size());
        for (var quantidade : quantidades) {
            resultado.add(quantidade);
        }
        return resultado;
    }

    /**
     * "Enche por igual": a cada rodada, divide o que falta pelas escolhas que
     * ainda têm questão sobrando, cada uma limitada ao que tem. Quem esgota sai da
     * rodada seguinte. Termina porque cada rodada ou distribui algo ou acusa falta.
     */
    private void distribuirPorIgual(int total, int restante, List<Integer> livres, int[] quantidades, List<Cota> cotas) {
        while (restante > 0) {
            var comEspaco = livres.stream()
                    .filter(i -> quantidades[i] < cotas.get(i).disponiveis())
                    .toList();
            if (comEspaco.isEmpty()) {
                // As fixas já foram conferidas contra o disponível; o que existe é
                // a soma delas mais tudo o que as escolhas livres têm
                long existentes = total - restante - somaDasLivres(livres, quantidades)
                        + livres.stream().mapToLong(k -> cotas.get(k).disponiveis()).sum();
                throw new QuestoesInsuficientesException(List.of(
                        "pediu %d questões, há %d nas escolhas e anos pedidos".formatted(total, existentes)));
            }

            int porEscolha = restante / comEspaco.size();
            if (porEscolha == 0) {
                // Sobra menor que o nº de escolhas: 1 a mais pra escolhas sorteadas
                var embaralhadas = new ArrayList<>(comEspaco);
                for (int k = embaralhadas.size() - 1; k > 0; k--) {
                    int j = aleatorio.nextInt(k + 1);
                    var troca = embaralhadas.get(k);
                    embaralhadas.set(k, embaralhadas.get(j));
                    embaralhadas.set(j, troca);
                }
                for (int k = 0; k < restante; k++) {
                    quantidades[embaralhadas.get(k)]++;
                }
                return;
            }
            for (var i : comEspaco) {
                int cabe = (int) Math.min(porEscolha, cotas.get(i).disponiveis() - quantidades[i]);
                quantidades[i] += cabe;
                restante -= cabe;
            }
        }
    }

    private static long somaDasLivres(List<Integer> livres, int[] quantidades) {
        return livres.stream().mapToLong(i -> quantidades[i]).sum();
    }
}
