package br.com.simulados.api.questao.infrastructure.habilidades;

import br.com.simulados.api.questao.application.HabilidadesInvalidasException;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unitário puro: conversão de texto, sem Spring nem banco
class LeitorDeHabilidadesCsvTest {

    private static final String CABECALHO = "ano,numero,idioma,area,habilidade,dificuldade,item_inep";

    private final LeitorDeHabilidadesCsv leitor = new LeitorDeHabilidadesCsv();

    @Test
    void converteLinhaCompletaESoDificuldade() {
        var linhas = leitor.converter(List.of(
                CABECALHO,
                "2015,127,,LC,21,2.39167,6830",
                "2009,1,,CN,,-1.70677,60083",
                "2021,1,INGLES,LC,6,0.5,1"));

        assertThat(linhas).hasSize(3);
        var primeira = linhas.getFirst();
        assertThat(primeira.linha()).isEqualTo(2);
        assertThat(primeira.area()).isEqualTo(Area.LINGUAGENS);
        assertThat(primeira.habilidade()).isEqualTo((short) 21);
        assertThat(primeira.dificuldade()).isEqualTo(2.39167);
        assertThat(linhas.get(1).habilidade()).isNull();
        assertThat(linhas.get(1).dificuldade()).isEqualTo(-1.70677);
        assertThat(linhas.get(2).idioma()).isEqualTo(Idioma.INGLES);
    }

    @Test
    void juntaTodosOsProblemasNumaExcecaoSo() {
        assertThatThrownBy(() -> leitor.converter(List.of(
                CABECALHO,
                "2015,127,,LC,31,2.3,1",
                "2015,128,,XX,21,2.3,1",
                "2015,129,,LC,21,NaN,1",
                "2015,130,,LC,,,1",
                "2015,131,,LC,21")))
                .isInstanceOf(HabilidadesInvalidasException.class)
                .satisfies(e -> assertThat(((HabilidadesInvalidasException) e).getProblemas()).hasSize(5))
                .hasMessageContaining("linha 2: habilidade inválida")
                .hasMessageContaining("linha 3: área inválida")
                .hasMessageContaining("linha 4: dificuldade inválida")
                .hasMessageContaining("linha 5: sem habilidade e sem dificuldade")
                .hasMessageContaining("linha 6: esperava 7 colunas");
    }

    @Test
    void recusaCabecalhoDiferente() {
        assertThatThrownBy(() -> leitor.converter(List.of("ano,numero,habilidade")))
                .isInstanceOf(HabilidadesInvalidasException.class)
                .hasMessageContaining("cabeçalho esperado");
    }
}
