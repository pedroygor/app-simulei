import type { Aproveitamento, Desempenho, DesempenhoNaArea, DesempenhoNoAssunto } from "@/lib/api/desempenho";

export type AreaComAssuntos = DesempenhoNaArea & { assuntos: DesempenhoNoAssunto[] };

/**
 * Agrupa os assuntos dentro da área, mantendo as duas ordens que vieram do backend
 * (áreas na do ENEM, assuntos na da taxonomia). Área sem nenhum assunto (só
 * questões sem classificação) aparece com a lista vazia, em vez de sumir: o
 * aproveitamento dela é real.
 */
export function agruparPorArea(desempenho: Pick<Desempenho, "porArea" | "porAssunto">): AreaComAssuntos[] {
  return desempenho.porArea.map((area) => ({
    ...area,
    assuntos: desempenho.porAssunto.filter((a) => a.area === area.area),
  }));
}

/** Quantas respostas faltam pra o assunto virar diagnóstico; 0 quando já basta. */
export function faltamRespostas(aproveitamento: Pick<Aproveitamento, "respondidas">, amostraMinima: number): number {
  return Math.max(0, amostraMinima - aproveitamento.respondidas);
}

/**
 * Separa os assuntos que já dá pra medir dos que ainda não têm a amostra mínima.
 * Com poucas provas quase todo assunto está abaixo do mínimo, e listar todos com
 * aviso esconderia justamente os que já dizem algo.
 */
export function separarPorAmostra(assuntos: DesempenhoNoAssunto[]): {
  medidos: DesempenhoNoAssunto[];
  semAmostra: DesempenhoNoAssunto[];
} {
  return {
    medidos: assuntos.filter((a) => a.aproveitamento.amostraSuficiente),
    semAmostra: assuntos.filter((a) => !a.aproveitamento.amostraSuficiente),
  };
}

/** "1 de 2" — ou "só em branco", quando não houve resposta: "0 de 0" não diz nada. */
export function placarCurto(aproveitamento: Pick<Aproveitamento, "acertos" | "respondidas">): string {
  return aproveitamento.respondidas === 0 ? "só em branco" : `${aproveitamento.acertos} de ${aproveitamento.respondidas}`;
}
