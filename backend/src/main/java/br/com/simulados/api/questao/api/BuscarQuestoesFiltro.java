package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

        // Enriquecimento nosso, ainda não preenchido (a enem.dev não fornece).
        // O filtro existe pro contrato não mudar quando os dados chegarem.
        @Size(max = 40) String materia
) {

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

    // Normalizado aqui pra query poder comparar sem chamar lower() no
    // parâmetro — ver comentário no QuestaoRepository
    public String materiaNormalizada() {
        return materia == null || materia.isBlank() ? null : materia.trim().toLowerCase();
    }
}
