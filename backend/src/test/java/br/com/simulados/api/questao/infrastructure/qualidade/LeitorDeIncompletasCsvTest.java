package br.com.simulados.api.questao.infrastructure.qualidade;

import br.com.simulados.api.questao.application.IncompletasInvalidasException;
import br.com.simulados.api.questao.domain.Idioma;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unitário puro: conversão de texto, sem Spring nem banco
class LeitorDeIncompletasCsvTest {

    private static final String CABECALHO = "ano,numero,idioma,motivo";

    private final LeitorDeIncompletasCsv leitor = new LeitorDeIncompletasCsv();

    @Test
    void motivoComVirgulaFicaInteiroNoUltimoCampo() {
        var linhas = leitor.converter(List.of(
                CABECALHO,
                "2016,165,,sem texto-base na fonte: o comando cita dados, situação e figura",
                "2023,5,INGLES,cartum quebrado na fonte"));

        assertThat(linhas).hasSize(2);
        assertThat(linhas.getFirst().numero()).isEqualTo(165);
        assertThat(linhas.getFirst().idioma()).isNull();
        assertThat(linhas.getLast().idioma()).isEqualTo(Idioma.INGLES);
    }

    @Test
    void motivoEObrigatorio() {
        // Uma marcação sem motivo é uma questão sumindo sem ninguém saber por quê
        assertThatThrownBy(() -> leitor.converter(List.of(CABECALHO, "2016,165,,", "2016,1x,,motivo")))
                .isInstanceOf(IncompletasInvalidasException.class)
                .hasMessageContaining("linha 2: esperava")
                .hasMessageContaining("linha 3: numero inválido");
    }
}
