package br.com.simulados.api.desempenho.domain;

import br.com.simulados.api.questao.domain.Area;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PontosFracosTest {

    @Test
    void soMedidosDoMenorAcertoProMaiorNoMaximoTres() {
        var assuntos = List.of(
                assunto("a", 10, 9),
                assunto("b", 2, 0),    // 0%, mas sem amostra: fica de fora
                assunto("c", 5, 1),
                assunto("d", 8, 4),
                assunto("e", 6, 2),
                assunto("f", 5, 5));

        assertThat(PontosFracos.de(assuntos)).extracting(AproveitamentoNoAssunto::assuntoSlug)
                .containsExactly("c", "e", "d");
    }

    @Test
    void semAssuntoMedidoNaoHaPontoFraco() {
        assertThat(PontosFracos.de(List.of(assunto("a", 4, 0)))).isEmpty();
        assertThat(PontosFracos.de(List.of())).isEmpty();
    }

    private static AproveitamentoNoAssunto assunto(String slug, long respondidas, long acertos) {
        return new AproveitamentoNoAssunto(Area.MATEMATICA, "matematica", "Matemática", slug, slug,
                new Aproveitamento(respondidas, acertos, 0));
    }
}
