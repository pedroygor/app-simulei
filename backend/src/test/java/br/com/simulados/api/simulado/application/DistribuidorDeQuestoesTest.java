package br.com.simulados.api.simulado.application;

import br.com.simulados.api.simulado.application.DistribuidorDeQuestoes.Cota;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unitário puro: aritmética, sem Spring nem banco. Semente fixa no gerador pra o
// sorteio da sobra ser reproduzível
class DistribuidorDeQuestoesTest {

    private final DistribuidorDeQuestoes distribuidor = new DistribuidorDeQuestoes(new Random(42));

    @Test
    void divideOTotalPorIgualEASobraVaiPraUmaSo() {
        var quantidades = distribuidor.distribuir(10, List.of(livre("Estatística", 50), livre("Probabilidade", 80),
                livre("Função do 1º grau", 40)));

        assertThat(quantidades).containsExactlyInAnyOrder(4, 3, 3);
    }

    @Test
    void escolhaComPoucasQuestoesRecebeOQueTemEORestoVaiProsOutros() {
        // Física moderna tem 1 questão na base inteira
        var quantidades = distribuidor.distribuir(10, List.of(livre("Física moderna", 1), livre("Cinemática", 30),
                livre("Dinâmica", 30)));

        assertThat(quantidades.getFirst()).isEqualTo(1);
        assertThat(quantidades.subList(1, 3)).containsExactlyInAnyOrder(5, 4);
    }

    @Test
    void fixaSaiPrimeiroEORestoSeDistribui() {
        var quantidades = distribuidor.distribuir(10, List.of(fixa("Probabilidade", 5, 80), livre("Estatística", 50),
                livre("Geometria plana", 50)));

        assertThat(quantidades.getFirst()).isEqualTo(5);
        assertThat(quantidades.subList(1, 3)).containsExactlyInAnyOrder(3, 2);
    }

    @Test
    void areasInteirasMultidisciplinar() {
        var quantidades = distribuidor.distribuir(90, List.of(livre("Ciências da Natureza", 600),
                livre("Matemática", 650)));

        assertThat(quantidades).containsExactly(45, 45);
    }

    @Test
    void todaEscolhaRecebePeloMenosUmaESomaSempreOTotal() {
        // Propriedade, em vários tamanhos: nunca 0 pra uma escolha, soma exata
        for (int total = 5; total <= 30; total++) {
            var cotas = IntStream.range(0, 5).mapToObj(i -> livre("A" + i, 3 + i * 7L)).toList();
            var quantidades = distribuidor.distribuir(total, cotas);

            assertThat(quantidades).allMatch(q -> q >= 1);
            assertThat(quantidades.stream().mapToInt(Integer::intValue).sum()).isEqualTo(total);
            for (int i = 0; i < cotas.size(); i++) {
                assertThat((long) quantidades.get(i)).isLessThanOrEqualTo(cotas.get(i).disponiveis());
            }
        }
    }

    @Test
    void maisEscolhasQueQuestoesNaoCabe() {
        var cotas = IntStream.range(0, 11).mapToObj(i -> livre("A" + i, 10)).toList();

        assertThatThrownBy(() -> distribuidor.distribuir(10, cotas))
                .isInstanceOf(PedidoDeSimuladoInvalidoException.class)
                .hasMessageContaining("11 escolhas não cabem numa prova de 10 questões");
    }

    @Test
    void escolhaSemNenhumaQuestaoNosAnosEhRecusadaPeloNome() {
        assertThatThrownBy(() -> distribuidor.distribuir(10, List.of(livre("Física moderna", 0), livre("Dinâmica", 30))))
                .isInstanceOf(QuestoesInsuficientesException.class)
                .hasMessageContaining("Física moderna não tem questões nos anos escolhidos");
    }

    @Test
    void fixaMaiorQueODisponivelDizQuantoPediuEQuantoHa() {
        assertThatThrownBy(() -> distribuidor.distribuir(10, List.of(fixa("Probabilidade", 8, 6), livre("Estatística", 50))))
                .isInstanceOf(QuestoesInsuficientesException.class)
                .hasMessageContaining("Probabilidade: pediu 8, há 6");
    }

    @Test
    void totalMaiorQueTudoQueExisteDizQuantoHa() {
        assertThatThrownBy(() -> distribuidor.distribuir(20, List.of(fixa("Probabilidade", 3, 80), livre("Física moderna", 1),
                livre("Eletrostática", 3))))
                .isInstanceOf(QuestoesInsuficientesException.class)
                .hasMessageContaining("pediu 20 questões, há 7");
    }

    @Test
    void fixasQueNaoDeixamUmaPraCadaLivre() {
        assertThatThrownBy(() -> distribuidor.distribuir(10, List.of(fixa("Probabilidade", 10, 80), livre("Estatística", 50))))
                .isInstanceOf(PedidoDeSimuladoInvalidoException.class)
                .hasMessageContaining("não sobra pelo menos 1");
    }

    @Test
    void soFixasPrecisamFecharOTotal() {
        assertThatThrownBy(() -> distribuidor.distribuir(10, List.of(fixa("Probabilidade", 4, 80), fixa("Estatística", 4, 50))))
                .isInstanceOf(PedidoDeSimuladoInvalidoException.class)
                .hasMessageContaining("somam 8, mas o total da prova é 10");
    }

    private static Cota livre(String rotulo, long disponiveis) {
        return new Cota(rotulo, null, disponiveis);
    }

    private static Cota fixa(String rotulo, int quantidade, long disponiveis) {
        return new Cota(rotulo, quantidade, disponiveis);
    }
}
