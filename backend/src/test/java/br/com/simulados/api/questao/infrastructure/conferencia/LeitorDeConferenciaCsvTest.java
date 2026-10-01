package br.com.simulados.api.questao.infrastructure.conferencia;

import br.com.simulados.api.questao.application.ConferenciaInvalidaException;
import br.com.simulados.api.questao.application.TipoDeCorrecao;
import br.com.simulados.api.questao.domain.Idioma;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unitário puro: conversão de texto, sem Spring nem banco
class LeitorDeConferenciaCsvTest {

    private static final String CABECALHO = "tipo,ano,numero,idioma,valor_banco,valor_inep,evidencia,detalhe";

    private final LeitorDeConferenciaCsv leitor = new LeitorDeConferenciaCsv();

    @Test
    void converteGabaritoEAnulada() {
        var linhas = leitor.converter(List.of(
                CABECALHO,
                "GABARITO,2021,15,,E,D,microdados INEP (ITENS_PROVA),item INEP 120646",
                "ANULADA,2018,163,,A,X,microdados INEP (ITENS_PROVA) + PDF oficial,Anulado"));

        assertThat(linhas).hasSize(2);
        var primeira = linhas.getFirst();
        assertThat(primeira.linha()).isEqualTo(2);
        assertThat(primeira.tipo()).isEqualTo(TipoDeCorrecao.GABARITO);
        assertThat(primeira.ano()).isEqualTo(2021);
        assertThat(primeira.numero()).isEqualTo(15);
        assertThat(primeira.idioma()).isNull();
        assertThat(primeira.valorBanco()).isEqualTo("E");
        assertThat(primeira.valorInep()).isEqualTo("D");
        assertThat(linhas.getLast().tipo()).isEqualTo(TipoDeCorrecao.ANULADA);
        assertThat(linhas.getLast().valorInep()).isEqualTo("X");
    }

    @Test
    void lePrimeirosSeisCamposMesmoComVirgulaEntreAspasNoTextoLivre() {
        // `evidencia` e `detalhe` são texto livre e o csv do Python põe aspas quando
        // têm vírgula. Ficam depois dos seis campos que importam, então não atrapalham
        var linhas = leitor.converter(List.of(
                CABECALHO,
                "GABARITO,2012,165,,E,C,\"PDF oficial, caderno azul\",\"item 7031, conferido\""));

        assertThat(linhas.getFirst().valorBanco()).isEqualTo("E");
        assertThat(linhas.getFirst().valorInep()).isEqualTo("C");
    }

    @Test
    void anuladaDeQuestaoQueOBancoNaoTinhaGabaritoAceitaValorBancoVazio() {
        var linhas = leitor.converter(List.of(CABECALHO, "ANULADA,2009,101,,,X,microdados,Anulado"));

        assertThat(linhas.getFirst().valorBanco()).isEmpty();
    }

    @Test
    void leIdioma() {
        var linhas = leitor.converter(List.of(CABECALHO, "GABARITO,2021,1,INGLES,C,B,x,y"));

        assertThat(linhas.getFirst().idioma()).isEqualTo(Idioma.INGLES);
    }

    @Test
    void ignoraLinhaEmBranco() {
        var linhas = leitor.converter(List.of(CABECALHO, "GABARITO,2021,15,,E,D,x,y", ""));

        assertThat(linhas).hasSize(1);
    }

    @Test
    void recusaCabecalhoDiferente() {
        // Coluna trocada de lugar leria a letra como ano: melhor parar
        assertThatThrownBy(() -> leitor.converter(List.of("tipo,numero,ano,idioma,valor_banco,valor_inep,evidencia,detalhe")))
                .isInstanceOf(ConferenciaInvalidaException.class)
                .hasMessageContaining("cabeçalho");
    }

    @Test
    void juntaTodosOsErrosDeFormatoComONumeroDaLinha() {
        assertThatThrownBy(() -> leitor.converter(List.of(
                CABECALHO,
                "TROCAR,2021,15,,E,D,x,y",
                "GABARITO,20x1,15,,E,D,x,y",
                "GABARITO,2021,15,FRANCES,E,D,x,y",
                "GABARITO,2021,15")))
                .isInstanceOfSatisfying(ConferenciaInvalidaException.class, e -> {
                    assertThat(e.getProblemas()).hasSize(4);
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 2: tipo"));
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 3: ano"));
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 4: idioma"));
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 5: esperava"));
                });
    }
}
