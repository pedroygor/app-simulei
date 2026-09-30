package br.com.simulados.api.questao.infrastructure.classificacao;

import br.com.simulados.api.questao.application.ClassificacaoInvalidaException;
import br.com.simulados.api.questao.application.LinhaDeClassificacao;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
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
 * Lê o dados/classificacao/assuntos.csv:
 *
 *   ano,numero,idioma,assunto,confianca,justificativa
 *   2019,88,,populacao-e-migracoes,alta,Bônus demográfico e estrutura etária
 *
 * Sem biblioteca de CSV, de propósito: as cinco primeiras colunas nunca têm
 * vírgula nem aspas (números, enum e slug), e a justificativa — única coluna
 * de texto livre, a que pode vir entre aspas com vírgula dentro — é a ÚLTIMA
 * e não vai pro banco. Um split limitado a 6 partes deixa tudo que sobrar
 * na sexta, sem precisar entender aspas.
 *
 * Erro de formato não para na primeira linha: junta todos e lança uma
 * ClassificacaoInvalidaException só, como o service faz na validação dele.
 */
@Component
public class LeitorDeClassificacaoCsv {

    private static final String CABECALHO = "ano,numero,idioma,assunto,confianca,justificativa";
    private static final int COLUNAS = 6;

    public List<LinhaDeClassificacao> ler(Path arquivo) {
        try {
            return converter(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + arquivo.toAbsolutePath(), e);
        }
    }

    List<LinhaDeClassificacao> converter(List<String> conteudo) {
        if (conteudo.isEmpty() || !conteudo.getFirst().strip().equals(CABECALHO)) {
            throw new ClassificacaoInvalidaException(List.of("cabeçalho esperado: " + CABECALHO));
        }

        var linhas = new ArrayList<LinhaDeClassificacao>();
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
            throw new ClassificacaoInvalidaException(problemas);
        }
        return linhas;
    }

    private LinhaDeClassificacao converterLinha(int numeroDaLinha, String texto) {
        var colunas = texto.split(",", COLUNAS);
        if (colunas.length < COLUNAS) {
            throw new IllegalArgumentException("esperava %d colunas, veio %d".formatted(COLUNAS, colunas.length));
        }
        return new LinhaDeClassificacao(
                numeroDaLinha,
                inteiro("ano", colunas[0]),
                inteiro("numero", colunas[1]),
                idioma(colunas[2]),
                colunas[3].strip(),
                confianca(colunas[4]));
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

    // O CSV usa minúsculo (alta/media/baixa); o enum, maiúsculo
    private ConfiancaDaClassificacao confianca(String valor) {
        try {
            return ConfiancaDaClassificacao.valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("confiança inválida: '%s'".formatted(valor));
        }
    }
}
