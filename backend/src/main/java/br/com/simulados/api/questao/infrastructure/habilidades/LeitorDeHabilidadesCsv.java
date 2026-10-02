package br.com.simulados.api.questao.infrastructure.habilidades;

import br.com.simulados.api.questao.application.HabilidadesInvalidasException;
import br.com.simulados.api.questao.application.LinhaDeHabilidade;
import br.com.simulados.api.questao.domain.Area;
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
import java.util.Map;

/**
 * Lê o dados/conferencia-inep/habilidades.csv:
 *
 *   ano,numero,idioma,area,habilidade,dificuldade,item_inep
 *   2015,127,,LC,21,2.39167,6830
 *   2009,1,,CN,,-1.70677,60083        (2009 fora de Matemática: só a dificuldade)
 *
 * Sem biblioteca de CSV: nenhum campo tem vírgula (números, siglas, idioma), e o
 * arquivo é gerado por script, nunca editado à mão. Erro de formato não para na
 * primeira linha: junta todos numa HabilidadesInvalidasException só.
 */
@Component
public class LeitorDeHabilidadesCsv {

    private static final String CABECALHO = "ano,numero,idioma,area,habilidade,dificuldade,item_inep";
    private static final int COLUNAS = 7;

    // SG_AREA dos microdados do INEP → a nossa área
    private static final Map<String, Area> AREA_DO_INEP = Map.of(
            "LC", Area.LINGUAGENS,
            "CH", Area.CIENCIAS_HUMANAS,
            "CN", Area.CIENCIAS_NATUREZA,
            "MT", Area.MATEMATICA);

    public List<LinhaDeHabilidade> ler(Path arquivo) {
        try {
            return converter(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + arquivo.toAbsolutePath(), e);
        }
    }

    List<LinhaDeHabilidade> converter(List<String> conteudo) {
        if (conteudo.isEmpty() || !conteudo.getFirst().strip().equals(CABECALHO)) {
            throw new HabilidadesInvalidasException(List.of("cabeçalho esperado: " + CABECALHO));
        }

        var linhas = new ArrayList<LinhaDeHabilidade>();
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
            throw new HabilidadesInvalidasException(problemas);
        }
        return linhas;
    }

    private LinhaDeHabilidade converterLinha(int numeroDaLinha, String texto) {
        // -1: mantém as colunas vazias do fim (sem ele, "…,,60083" perderia posições)
        var colunas = texto.split(",", -1);
        if (colunas.length != COLUNAS) {
            throw new IllegalArgumentException("esperava %d colunas, veio %d".formatted(COLUNAS, colunas.length));
        }
        var habilidade = habilidade(colunas[4]);
        var dificuldade = dificuldade(colunas[5]);
        if (habilidade == null && dificuldade == null) {
            throw new IllegalArgumentException("sem habilidade e sem dificuldade: a linha não tem o que gravar");
        }
        return new LinhaDeHabilidade(
                numeroDaLinha,
                inteiro("ano", colunas[0]),
                inteiro("numero", colunas[1]),
                idioma(colunas[2]),
                area(colunas[3]),
                habilidade,
                dificuldade);
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

    private Area area(String valor) {
        var area = AREA_DO_INEP.get(valor.strip());
        if (area == null) {
            throw new IllegalArgumentException("área inválida: '%s' (esperado LC, CH, CN ou MT)".formatted(valor));
        }
        return area;
    }

    // Vazio = o INEP não publicou. Fora de 1–30 é recusado aqui, antes da FK da V8
    private Short habilidade(String valor) {
        var limpo = valor.strip();
        if (limpo.isEmpty()) {
            return null;
        }
        try {
            var codigo = Short.parseShort(limpo);
            if (codigo < 1 || codigo > 30) {
                throw new NumberFormatException();
            }
            return codigo;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("habilidade inválida: '%s' (esperado 1–30)".formatted(valor));
        }
    }

    // Parâmetro B da TRI, com ponto decimal. NaN e infinito são recusados:
    // Double.parseDouble aceita "NaN", e isso não é uma dificuldade
    private Double dificuldade(String valor) {
        var limpo = valor.strip();
        if (limpo.isEmpty()) {
            return null;
        }
        try {
            var numero = Double.parseDouble(limpo);
            if (!Double.isFinite(numero)) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("dificuldade inválida: '%s'".formatted(valor));
        }
    }
}
