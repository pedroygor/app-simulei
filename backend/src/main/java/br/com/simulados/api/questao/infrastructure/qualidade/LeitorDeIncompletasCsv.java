package br.com.simulados.api.questao.infrastructure.qualidade;

import br.com.simulados.api.questao.application.IncompletasInvalidasException;
import br.com.simulados.api.questao.application.LinhaDeIncompleta;
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
 * Lê o dados/qualidade/questoes_incompletas.csv:
 *
 *   ano,numero,idioma,motivo
 *   2016,165,,sem texto-base na fonte: o comando cita dados ou situação que não vieram
 *
 * Sem biblioteca de CSV: os três primeiros campos nunca têm vírgula, e o `motivo`
 * (texto livre, pode vir entre aspas) é o último e não vai pro banco — um split
 * limitado a 4 partes deixa tudo que sobrar nele.
 */
@Component
public class LeitorDeIncompletasCsv {

    private static final String CABECALHO = "ano,numero,idioma,motivo";

    public List<LinhaDeIncompleta> ler(Path arquivo) {
        try {
            return converter(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + arquivo.toAbsolutePath(), e);
        }
    }

    List<LinhaDeIncompleta> converter(List<String> conteudo) {
        if (conteudo.isEmpty() || !conteudo.getFirst().strip().equals(CABECALHO)) {
            throw new IncompletasInvalidasException(List.of("cabeçalho esperado: " + CABECALHO));
        }

        var linhas = new ArrayList<LinhaDeIncompleta>();
        var problemas = new ArrayList<String>();
        for (int i = 1; i < conteudo.size(); i++) {
            var texto = conteudo.get(i);
            var numeroDaLinha = i + 1;
            if (texto.isBlank()) {
                continue;
            }
            try {
                var colunas = texto.split(",", 4);
                if (colunas.length < 4 || colunas[3].isBlank()) {
                    throw new IllegalArgumentException("esperava ano,numero,idioma,motivo (o motivo é obrigatório)");
                }
                linhas.add(new LinhaDeIncompleta(numeroDaLinha,
                        inteiro("ano", colunas[0]), inteiro("numero", colunas[1]), idioma(colunas[2])));
            } catch (IllegalArgumentException e) {
                problemas.add("linha %d: %s".formatted(numeroDaLinha, e.getMessage()));
            }
        }

        if (!problemas.isEmpty()) {
            throw new IncompletasInvalidasException(problemas);
        }
        return linhas;
    }

    private int inteiro(String campo, String valor) {
        try {
            return Integer.parseInt(valor.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("%s inválido: '%s'".formatted(campo, valor));
        }
    }

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
