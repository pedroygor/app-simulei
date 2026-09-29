package br.com.simulados.api.questao.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadernoDoEnemTest {

    // As fronteiras de cada bloco (1, 45/46, 90/91, 135/136, 180) nos três
    // formatos de prova: é onde um erro de índice apareceria
    @ParameterizedTest(name = "{0}/{1} é {2}")
    @CsvSource({
            "2009,   1, CIENCIAS_NATUREZA",
            "2009,  45, CIENCIAS_NATUREZA",
            "2009,  46, CIENCIAS_HUMANAS",
            "2009,  90, CIENCIAS_HUMANAS",
            "2009,  91, LINGUAGENS",
            "2009, 135, LINGUAGENS",
            "2009, 136, MATEMATICA",
            "2009, 180, MATEMATICA",
            "2010,   1, CIENCIAS_HUMANAS",
            "2010,  46, CIENCIAS_NATUREZA",
            "2016,  91, LINGUAGENS",
            "2016, 180, MATEMATICA",
            "2017,   1, LINGUAGENS",
            "2017,  45, LINGUAGENS",
            "2017,  46, CIENCIAS_HUMANAS",
            "2017,  91, CIENCIAS_NATUREZA",
            "2017, 136, MATEMATICA",
            "2023, 180, MATEMATICA",
    })
    void areaSegueOBlocoDaPosicao(int ano, int numero, Area esperada) {
        assertThat(CadernoDoEnem.areaDaPosicao(ano, numero)).isEqualTo(esperada);
    }

    // Questões reais em que a enem.dev erra a área, conferidas pelo conteúdo
    // em 29/09/2026. Se alguém "simplificar" voltando a confiar na fonte,
    // estes quebram
    @ParameterizedTest(name = "{0}/{1} ({3}) é {2}")
    @CsvSource({
            "2022, 108, CIENCIAS_NATUREZA, ácido tartárico do vinho; a fonte diz Humanas",
            "2012, 164, MATEMATICA,        dosagem de remédio pela bula; a fonte diz Humanas",
            "2023, 130, CIENCIAS_NATUREZA, composição de copos; a fonte diz Matemática",
            "2018, 140, MATEMATICA,        volume de potes cilíndricos; a fonte diz Natureza",
            "2023,  81, CIENCIAS_HUMANAS,  economia das ilegalidades; a fonte diz Linguagens",
    })
    void corrigeOsErrosReaisDaFonte(int ano, int numero, Area esperada, String caso) {
        assertThat(CadernoDoEnem.areaDaPosicao(ano, numero)).isEqualTo(esperada);
    }

    @Test
    void anoFuturoSegueAOrdemAtual() {
        assertThat(CadernoDoEnem.areaDaPosicao(2026, 1)).isEqualTo(Area.LINGUAGENS);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 181})
    void recusaPosicaoForaDaProva(int numero) {
        assertThatThrownBy(() -> CadernoDoEnem.areaDaPosicao(2023, numero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora da prova");
    }
}
