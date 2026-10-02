package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;

/**
 * Uma linha do dados/conferencia-inep/habilidades.csv, já convertida. Mesmo motivo
 * da LinhaDeConferencia: o serviço não conhece arquivo, e o teste monta as linhas.
 *
 * `area` vem do INEP (SG_AREA). Não é gravada: serve pra conferir que o casamento
 * com a nossa numeração caiu na questão certa. `habilidade` e `dificuldade` são
 * nulas quando o INEP não publicou (2009 fora de Matemática tem só a dificuldade).
 */
public record LinhaDeHabilidade(
        int linha,
        int ano,
        int numero,
        Idioma idioma,
        Area area,
        Short habilidade,
        Double dificuldade
) {

    public String chave() {
        return "%d/%d%s".formatted(ano, numero, idioma == null ? "" : "/" + idioma);
    }
}
