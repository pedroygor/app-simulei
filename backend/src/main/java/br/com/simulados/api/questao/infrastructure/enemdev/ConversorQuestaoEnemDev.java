package br.com.simulados.api.questao.infrastructure.enemdev;

import br.com.simulados.api.questao.application.QuestaoInvalidaException;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.CadernoDoEnem;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Traduz o formato da enem.dev pro nosso domínio. Fica na infraestrutura de
 * propósito: é aqui que moram as manias da fonte (nomes de área em kebab-case,
 * idioma em minúsculo, imagens embutidas no markdown).
 *
 * Não faz I/O: quem baixa imagem é o importador, que passa a função de
 * reescrita de URL. Isso mantém o conversor puro e testável sem rede.
 */
@Component
public class ConversorQuestaoEnemDev {

    static final String FONTE = "enem.dev";
    private static final int TOTAL_DE_ALTERNATIVAS = 5;
    private static final List<String> LETRAS = List.of("A", "B", "C", "D", "E");

    // Captura a URL de imagens em markdown: ![alt](url)
    private static final Pattern IMAGEM_MARKDOWN = Pattern.compile("!\\[[^\\]]*\\]\\(([^)\\s]+)");

    public Questao converter(QuestaoEnemDev origem, UnaryOperator<String> reescreverImagem) {
        validar(origem);

        var questao = new Questao();
        questao.setAno(origem.year());
        questao.setNumero(origem.index());
        questao.setIdioma(idiomaDe(origem.language()));
        questao.setArea(areaPelaPosicao(origem));
        questao.setEnunciado(reescreverImagensDoMarkdown(origem.context(), reescreverImagem));
        questao.setComando(origem.alternativesIntroduction());
        questao.setGabarito(origem.correctAlternative());
        // A enem.dev não expõe questões anuladas — a flag existe no nosso
        // modelo pra curadoria futura, e por ora nasce sempre false
        questao.setAnulada(origem.correctAlternative() == null);
        questao.setFonte(FONTE);
        questao.setFonteId(fonteIdDe(origem));

        for (var alternativaOrigem : origem.alternatives()) {
            var alternativa = new Alternativa();
            alternativa.setLetra(alternativaOrigem.letter());
            alternativa.setTexto(alternativaOrigem.text());
            if (alternativaOrigem.file() != null) {
                alternativa.setImagemUrl(reescreverImagem.apply(alternativaOrigem.file()));
            }
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }

    // Identificador estável na origem, pra rastrear de onde veio cada questão
    static String fonteIdDe(QuestaoEnemDev origem) {
        return origem.language() == null
                ? "%d/%d".formatted(origem.year(), origem.index())
                : "%d/%d/%s".formatted(origem.year(), origem.index(), origem.language());
    }

    private void validar(QuestaoEnemDev origem) {
        var alternativas = origem.alternatives();
        if (alternativas == null || alternativas.size() != TOTAL_DE_ALTERNATIVAS) {
            // 7 questões da fonte vêm com 4 alternativas (falha de extração).
            // Questão com alternativa faltando não é a prova real — fora.
            throw new QuestaoInvalidaException(origem.year(), origem.index(),
                    "esperava %d alternativas, veio %d".formatted(
                            TOTAL_DE_ALTERNATIVAS, alternativas == null ? 0 : alternativas.size()));
        }
        for (var alternativa : alternativas) {
            if (vazio(alternativa.text()) && alternativa.file() == null) {
                // Ex: 2023 q132, alternativas A-D em branco. Sem conteúdo pra
                // mostrar, o aluno não tem como responder
                throw new QuestaoInvalidaException(origem.year(), origem.index(),
                        "alternativa %s sem texto e sem imagem".formatted(alternativa.letter()));
            }
        }
        if (vazio(origem.context()) && vazio(origem.alternativesIntroduction())) {
            throw new QuestaoInvalidaException(origem.year(), origem.index(),
                    "sem enunciado e sem comando");
        }
        var gabarito = origem.correctAlternative();
        if (gabarito != null && !LETRAS.contains(gabarito)) {
            throw new QuestaoInvalidaException(origem.year(), origem.index(),
                    "gabarito fora de A-E: " + gabarito);
        }
    }

    // As imagens vêm embutidas no markdown apontando pro host da enem.dev.
    // Trocamos por URLs nossas — senão a "independência da fonte" seria só do
    // texto, e a prova quebraria no dia em que o enem.dev saísse do ar.
    private String reescreverImagensDoMarkdown(String markdown, UnaryOperator<String> reescreverImagem) {
        if (markdown == null) {
            return null;
        }
        var resultado = new StringBuilder();
        var matcher = IMAGEM_MARKDOWN.matcher(markdown);
        var fim = 0;
        while (matcher.find()) {
            var urlOriginal = matcher.group(1);
            resultado.append(markdown, fim, matcher.start(1))
                    .append(reescreverImagem.apply(urlOriginal));
            fim = matcher.end(1);
        }
        return resultado.append(markdown.substring(fim)).toString();
    }

    private Idioma idiomaDe(String language) {
        if (language == null) {
            return null;
        }
        return switch (language) {
            case "ingles" -> Idioma.INGLES;
            case "espanhol" -> Idioma.ESPANHOL;
            default -> throw new IllegalArgumentException("Idioma desconhecido na fonte: " + language);
        };
    }

    // A área vem da POSIÇÃO no caderno, não do "discipline" da fonte, que
    // erra 5% das questões (ver CadernoDoEnem)
    private Area areaPelaPosicao(QuestaoEnemDev origem) {
        try {
            return CadernoDoEnem.areaDaPosicao(origem.year(), origem.index());
        } catch (IllegalArgumentException e) {
            throw new QuestaoInvalidaException(origem.year(), origem.index(), e.getMessage());
        }
    }

    /**
     * O que a fonte DIZ que é a área. Não decide nada: serve pro importador
     * medir quanto a fonte discorda da posição. Vazio quando a fonte não
     * informa ou manda um nome que não conhecemos.
     */
    public Optional<Area> areaDeclaradaPelaFonte(QuestaoEnemDev origem) {
        if (origem.discipline() == null) {
            return Optional.empty();
        }
        return switch (origem.discipline()) {
            case "linguagens" -> Optional.of(Area.LINGUAGENS);
            case "ciencias-humanas" -> Optional.of(Area.CIENCIAS_HUMANAS);
            case "ciencias-natureza" -> Optional.of(Area.CIENCIAS_NATUREZA);
            case "matematica" -> Optional.of(Area.MATEMATICA);
            default -> Optional.empty();
        };
    }

    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
