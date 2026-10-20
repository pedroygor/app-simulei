package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Filtros da busca de questões. Chegam como query params, não como corpo — mas
 * validação continua obrigatória: `?ano=abc` ou `?anoInicial=1500` são entrada
 * de usuário igual a qualquer outra.
 *
 * Todos os campos são opcionais (null = "não filtre por isso"). Os enums o
 * Spring converte sozinho a partir do texto; valor fora do enum vira 400 no
 * GlobalExceptionHandler, não 500.
 */
public record BuscarQuestoesFiltro(
        Area area,

        Idioma idioma,

        // A fonte tem provas de 2009 em diante; o teto folgado evita ter que
        // mexer aqui todo ano
        @Min(2009) @Max(2030) Integer anoInicial,

        @Min(2009) @Max(2030) Integer anoFinal,

        // Slugs da taxonomia (docs/taxonomia.md): minúsculas, números e hífen.
        // Validar o FORMATO aqui devolve 400 pra entrada malformada em vez de
        // uma busca vazia sem explicação. Slug bem formado mas inexistente
        // (?assunto=nao-existe) é só uma busca sem resultado — não é erro
        // Teto de 60 = tamanho da coluna slug em assuntos (V4)
        @Size(max = 60) @Pattern(regexp = SLUG, message = SLUG_INVALIDO) String materia,

        @Size(max = 60) @Pattern(regexp = SLUG, message = SLUG_INVALIDO) String assunto
) {

    public static final String SLUG = "[a-z0-9]+(-[a-z0-9]+)*";
    public static final String SLUG_INVALIDO = "deve ser um identificador da taxonomia (ex.: probabilidade)";

    // Tolera o usuário mandando o intervalo invertido (?anoInicial=2023&anoFinal=2010)
    // em vez de devolver lista vazia sem explicação.
    //
    // Escrito com if e não com ternário de propósito: num ternário onde um ramo
    // é int (Math.min) e o outro é Integer, o Java unifica os tipos em int e
    // desempacota o ramo Integer — o que estoura NullPointerException quando o
    // valor é null. É a armadilha clássica de autoboxing.
    public Integer menorAno() {
        if (anoInicial == null || anoFinal == null) {
            return anoInicial;
        }
        return Math.min(anoInicial, anoFinal);
    }

    public Integer maiorAno() {
        if (anoInicial == null || anoFinal == null) {
            return anoFinal;
        }
        return Math.max(anoInicial, anoFinal);
    }
}
