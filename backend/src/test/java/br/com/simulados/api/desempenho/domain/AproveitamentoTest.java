package br.com.simulados.api.desempenho.domain;

import br.com.simulados.api.simulado.domain.Placar;
import org.junit.jupiter.api.Test;

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
}
