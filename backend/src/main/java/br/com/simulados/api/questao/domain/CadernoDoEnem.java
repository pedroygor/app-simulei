package br.com.simulados.api.questao.domain;

import java.util.List;

/**
 * A área de uma questão do ENEM é definida pela POSIÇÃO dela na prova: cada
 * área ocupa um bloco fixo de 45 questões, e a ordem dos blocos mudou duas
 * vezes na história do exame.
 *
 * Existe porque a enem.dev erra a área de 147 das 2.741 questões (5,4%) —
 * ácido do vinho e dosagem de remédio marcados como Ciências Humanas, por
 * exemplo. A posição é o fato; o "discipline" da fonte é só uma etiqueta.
 * Conferido contra o banco inteiro em 29/09/2026: com esta regra, nenhuma área
 * de nenhum ano passa de 45 questões por variante, e as divergências
 * inspecionadas batem com o conteúdo (ver CLAUDE.md, fatos da fonte).
 */
public final class CadernoDoEnem {

    public static final int QUESTOES_POR_AREA = 45;
    public static final int TOTAL_DE_QUESTOES = 4 * QUESTOES_POR_AREA;

    // 2009: prova reformulada, primeiro dia Natureza + Humanas
    private static final List<Area> ORDEM_2009 = List.of(
            Area.CIENCIAS_NATUREZA, Area.CIENCIAS_HUMANAS, Area.LINGUAGENS, Area.MATEMATICA);
    private static final List<Area> ORDEM_2010_A_2016 = List.of(
            Area.CIENCIAS_HUMANAS, Area.CIENCIAS_NATUREZA, Area.LINGUAGENS, Area.MATEMATICA);
    // 2017 em diante: Linguagens + Humanas no primeiro dia. Ano futuro assume
    // esta ordem; se o ENEM mudar de novo, o importador acusa a divergência
    // em massa com a fonte (ver ImportadorDeQuestoes)
    private static final List<Area> ORDEM_A_PARTIR_DE_2017 = List.of(
            Area.LINGUAGENS, Area.CIENCIAS_HUMANAS, Area.CIENCIAS_NATUREZA, Area.MATEMATICA);

    private CadernoDoEnem() {
    }

    public static Area areaDaPosicao(int ano, int numero) {
        if (numero < 1 || numero > TOTAL_DE_QUESTOES) {
            throw new IllegalArgumentException(
                    "posição %d fora da prova (1 a %d)".formatted(numero, TOTAL_DE_QUESTOES));
        }
        return ordemDoAno(ano).get((numero - 1) / QUESTOES_POR_AREA);
    }

    private static List<Area> ordemDoAno(int ano) {
        if (ano <= 2009) {
            return ORDEM_2009;
        }
        return ano <= 2016 ? ORDEM_2010_A_2016 : ORDEM_A_PARTIR_DE_2017;
    }
}
