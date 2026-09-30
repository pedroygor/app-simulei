package br.com.simulados.api.questao.infrastructure.classificacao;

import br.com.simulados.api.questao.application.ClassificacaoInvalidaException;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.Idioma;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unitário puro: conversão de texto, sem Spring nem banco
class LeitorDeClassificacaoCsvTest {

    private static final String CABECALHO = "ano,numero,idioma,assunto,confianca,justificativa";

    private final LeitorDeClassificacaoCsv leitor = new LeitorDeClassificacaoCsv();

    @Test
    void converteLinhaSemIdiomaEComIdioma() {
        var linhas = leitor.converter(List.of(
                CABECALHO,
                "2019,88,,populacao-e-migracoes,alta,Bônus demográfico",
                "2023,1,INGLES,interpretacao-ingles,media,Texto em inglês"));

        assertThat(linhas).hasSize(2);
        var primeira = linhas.getFirst();
        assertThat(primeira.linha()).isEqualTo(2);
        assertThat(primeira.ano()).isEqualTo(2019);
        assertThat(primeira.numero()).isEqualTo(88);
        assertThat(primeira.idioma()).isNull();
        assertThat(primeira.assunto()).isEqualTo("populacao-e-migracoes");
        assertThat(primeira.confianca()).isEqualTo(ConfiancaDaClassificacao.ALTA);
        assertThat(linhas.getLast().idioma()).isEqualTo(Idioma.INGLES);
    }

    @Test
    void justificativaEntreAspasComVirgulaNaoQuebraAsColunas() {
        // O csv do Python põe aspas quando a justificativa tem vírgula
        var linhas = leitor.converter(List.of(
                CABECALHO,
                "2009,49,,idade-media,baixa,\"Memória medieval, poderia ser patrimonio\""));

        assertThat(linhas.getFirst().assunto()).isEqualTo("idade-media");
        assertThat(linhas.getFirst().confianca()).isEqualTo(ConfiancaDaClassificacao.BAIXA);
    }

    @Test
    void ignoraLinhaEmBranco() {
        var linhas = leitor.converter(List.of(CABECALHO, "2019,88,,populacao-e-migracoes,alta,x", ""));

        assertThat(linhas).hasSize(1);
    }

    @Test
    void recusaCabecalhoDiferente() {
        // Coluna trocada de lugar leria o slug como idioma: melhor parar
        assertThatThrownBy(() -> leitor.converter(List.of("ano,numero,assunto,idioma,confianca,justificativa")))
                .isInstanceOf(ClassificacaoInvalidaException.class)
                .hasMessageContaining("cabeçalho");
    }

    @Test
    void juntaTodosOsErrosDeFormatoComONumeroDaLinha() {
        assertThatThrownBy(() -> leitor.converter(List.of(
                CABECALHO,
                "20x9,88,,populacao-e-migracoes,alta,x",
                "2019,88,FRANCES,populacao-e-migracoes,alta,x",
                "2019,88,,populacao-e-migracoes,altissima,x",
                "2019,88,,populacao-e-migracoes")))
                .isInstanceOfSatisfying(ClassificacaoInvalidaException.class, e -> {
                    assertThat(e.getProblemas()).hasSize(4);
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 2: ano"));
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 3: idioma"));
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 4: confiança"));
                    assertThat(e.getProblemas()).anyMatch(p -> p.startsWith("linha 5: esperava"));
                });
    }
}
