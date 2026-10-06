package br.com.simulados.api.simulado.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// Java puro: a regra do percentual e da ordenação não depende de banco
class PlacarTest {

    @Test
    void percentualArredondaPraBaixoESemQuestaoEhNulo() {
        assertThat(new Placar(3, 3, 2).percentual()).isEqualTo(66);
        assertThat(new Placar(5, 5, 5).percentual()).isEqualTo(100);
        // 0 questões: nada pra errar, não 0% (seção 8.4)
        assertThat(Placar.VAZIO.percentual()).isNull();
    }

    @Test
    void emBrancoEhOQueNaoFoiRespondido() {
        assertThat(new Placar(10, 7, 4).emBranco()).isEqualTo(3);
    }

    @Test
    void piorPrimeiroComparaFracaoSemArredondar() {
        var umDeTres = new Placar(3, 3, 1);       // 33,33…%
        var trintaETresDeCem = new Placar(100, 100, 33); // 33%
        var tudoCerto = new Placar(2, 2, 2);

        var ordenados = Stream.of(tudoCerto, umDeTres, Placar.VAZIO, trintaETresDeCem)
                .sorted(Placar.PIOR_PRIMEIRO)
                .toList();

        // Com percentual inteiro os dois primeiros empatariam em 33
        assertThat(ordenados).isEqualTo(List.of(trintaETresDeCem, umDeTres, tudoCerto, Placar.VAZIO));
    }
}
