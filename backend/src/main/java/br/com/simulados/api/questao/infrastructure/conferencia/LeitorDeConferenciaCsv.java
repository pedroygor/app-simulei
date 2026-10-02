package br.com.simulados.api.questao.infrastructure.conferencia;

import br.com.simulados.api.questao.application.ConferenciaInvalidaException;
import br.com.simulados.api.questao.application.LinhaDeConferencia;
import br.com.simulados.api.questao.application.TipoDeCorrecao;
import br.com.simulados.api.questao.domain.Idioma;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lê o dados/conferencia-inep/correcoes.csv:
 *
 *   tipo,ano,numero,idioma,valor_banco,valor_inep,evidencia,detalhe
 *   GABARITO,2021,15,,E,D,microdados INEP (ITENS_PROVA) + PDF oficial ...,item INEP 120646
 *   ANULADA,2018,163,,A,X,microdados INEP (ITENS_PROVA) + PDF oficial ...,Anulado
 *
 * Sem biblioteca de CSV, pelo mesmo motivo do leitor da classificação: os seis
 * primeiros campos nunca têm vírgula (enum, números, idioma, letras), e os dois
 * de texto livre — `evidencia` e `detalhe`, que podem vir entre aspas — vêm por
 * último e não vão pro banco. Um split limitado a 7 partes deixa tudo que sobrar
 * na sétima, sem precisar entender aspas.
 *
 * Erro de formato não para na primeira linha: junta todos e lança uma
 * ConferenciaInvalidaException só.
 */
@Component
public class LeitorDeConferenciaCsv {

    private static final String CABECALHO = "tipo,ano,numero,idioma,valor_banco,valor_inep,evidencia,detalhe";
    private static final int CAMPOS_LIDOS = 6;

    public List<LinhaDeConferencia> ler(Path arquivo) {
        try {
            return converter(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + arquivo.toAbsolutePath(), e);
        }
    }

    List<LinhaDeConferencia> converter(List<String> conteudo) {
        if (conteudo.isEmpty() || !conteudo.getFirst().strip().equals(CABECALHO)) {
            throw new ConferenciaInvalidaException(List.of("cabeçalho esperado: " + CABECALHO));
        }

        var linhas = new ArrayList<LinhaDeConferencia>();
        var problemas = new ArrayList<String>();

        // i = 1 pula o cabeçalho; numeroDaLinha conta como o editor mostra
        for (int i = 1; i < conteudo.size(); i++) {
            var texto = conteudo.get(i);
            var numeroDaLinha = i + 1;
            if (texto.isBlank()) {
                continue;
            }
            try {
                linhas.add(converterLinha(numeroDaLinha, texto));
            } catch (IllegalArgumentException e) {
                problemas.add("linha %d: %s".formatted(numeroDaLinha, e.getMessage()));
            }
        }

        if (!problemas.isEmpty()) {
            throw new ConferenciaInvalidaException(problemas);
        }
        return linhas;
    }

    private LinhaDeConferencia converterLinha(int numeroDaLinha, String texto) {
        var colunas = texto.split(",", CAMPOS_LIDOS + 1);
        if (colunas.length <= CAMPOS_LIDOS) {
            throw new IllegalArgumentException(
                    "esperava pelo menos %d colunas, veio %d".formatted(CAMPOS_LIDOS + 1, colunas.length));
        }
        return new LinhaDeConferencia(
                numeroDaLinha,
                tipo(colunas[0]),
                inteiro("ano", colunas[1]),
                inteiro("numero", colunas[2]),
                idioma(colunas[3]),
                colunas[4].strip(),
                colunas[5].strip());
    }

    private TipoDeCorrecao tipo(String valor) {
        try {
            return TipoDeCorrecao.valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("tipo inválido: '%s' (esperado GABARITO ou ANULADA)".formatted(valor));
        }
    }

    private int inteiro(String campo, String valor) {
        try {
            return Integer.parseInt(valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("%s inválido: '%s'".formatted(campo, valor));
        }
    }

    // Vazio = questão sem idioma (todas menos as de língua estrangeira)
    private Idioma idioma(String valor) {
        var limpo = valor.strip();
        if (limpo.isEmpty()) {
            return null;
        }
        try {
            return Idioma.valueOf(limpo.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("idioma inválido: '%s'".formatted(valor));
        }
    }
}
