package br.com.simulados.api.desempenho.domain;

import br.com.simulados.api.simulado.domain.Placar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AproveitamentoTest {

    @Test
    void percentualEhSobreAsRespondidasNaoSobreOTotal() {
        // 10 questões, 4 respondidas, 3 certas: a nota seria 30%, o aproveitamento 75%
        var aproveitamento = Aproveitamento.de(new Placar(10, 4, 3));

        assertThat(aproveitamento.percentual()).isEqualTo(75);
        assertThat(aproveitamento.emBranco()).isEqualTo(6);
    }

    @Test
    void semRespostaNaoTemPercentual() {
        // Tudo em branco não é "errou tudo": não há resposta pra medir
        assertThat(Aproveitamento.de(new Placar(3, 0, 0)).percentual()).isNull();
        assertThat(Aproveitamento.VAZIO.percentual()).isNull();
    }

    @Test
    void amostraContaRespondidasEPrecisaDeCinco() {
        assertThat(new Aproveitamento(4, 4, 20).amostraSuficiente()).isFalse();
        assertThat(new Aproveitamento(5, 0, 0).amostraSuficiente()).isTrue();
    }

    @Test
    void arredondaPraBaixoESomaTudo() {
        var soma = new Aproveitamento(2, 1, 1).somar(new Aproveitamento(1, 1, 3));

        assertThat(soma).isEqualTo(new Aproveitamento(3, 2, 4));
        assertThat(soma.percentual()).isEqualTo(66);
    }

    @Test
    void pertoDoChuteEhAteTrintaPorCentoNaFracaoExataEComAmostra() {
        assertThat(new Aproveitamento(10, 3, 0).pertoDoChute()).isTrue();    // 30% exato
        assertThat(new Aproveitamento(5, 1, 0).pertoDoChute()).isTrue();     // 20%, o próprio chute
        // 31 de 100: o percentual arredondado pra baixo seria 31, mas é a fração que decide
        assertThat(new Aproveitamento(100, 31, 0).pertoDoChute()).isFalse();
        assertThat(new Aproveitamento(10, 4, 0).pertoDoChute()).isFalse();
        // 1 de 4 é 25%, mas sem amostra ainda é sorte
        assertThat(new Aproveitamento(4, 1, 0).pertoDoChute()).isFalse();
        assertThat(Aproveitamento.VAZIO.pertoDoChute()).isFalse();
    }

    @Test
    void piorPrimeiroComparaFracoesENoEmpateQuemTemMaisRespostas() {
        var umDeTres = new Aproveitamento(3, 1, 0);           // 33,3%
        var trintaETresDeCem = new Aproveitamento(100, 33, 0); // 33,0% — pior, mesmo arredondando igual
        var metadeDeDez = new Aproveitamento(10, 5, 0);
        var metadeDeQuatro = new Aproveitamento(4, 2, 0);
        var semResposta = Aproveitamento.VAZIO;

        var ordenados = List.of(semResposta, metadeDeQuatro, umDeTres, metadeDeDez, trintaETresDeCem).stream()
                .sorted(Aproveitamento.PIOR_PRIMEIRO).toList();

        assertThat(ordenados).containsExactly(trintaETresDeCem, umDeTres, metadeDeDez, metadeDeQuatro, semResposta);
    }
}
