import type { Evolucao } from "@/lib/api/desempenho";
import type { ProvaAberta } from "@/lib/api/tentativas";

export type Tendencia = { atual: number; anterior: number; diferenca: number };

/**
 * Esta semana × semana passada (as duas últimas da evolução). Só com amostra
 * suficiente NAS DUAS (decidido em 20/10/2026): com 3 respostas, "você subiu 20
 * pontos" seria sorte contada como progresso. Sem isso, null — a tela não mostra
 * tendência nenhuma, em vez de uma falsa.
 */
export function tendenciaSemanal(evolucao: Evolucao): Tendencia | null {
  const semanas = evolucao.semanas;
  if (semanas.length < 2) return null;
  const atual = semanas[semanas.length - 1].geral;
  const anterior = semanas[semanas.length - 2].geral;
  if (!atual.amostraSuficiente || !anterior.amostraSuficiente) return null;
  if (atual.percentual === null || anterior.percentual === null) return null;
  return { atual: atual.percentual, anterior: anterior.percentual, diferenca: atual.percentual - anterior.percentual };
}

/** "12 de 45 respondidas" — sem acertos: a prova está aberta. */
export function progressoDaProva(prova: Pick<ProvaAberta, "questoes" | "respondidas">): string {
  return `${prova.respondidas} de ${prova.questoes} respondida${prova.questoes === 1 ? "" : "s"}`;
}

/**
 * Quando a prova termina, no fuso do aparelho: "termina às 15:40", ou com a data se
 * não for hoje. É o PRAZO do servidor (fato fixo), não uma contagem regressiva: não
 * envelhece com a tela aberta nem depende do relógio do celular estar certo.
 * `fuso` existe pro teste; na tela fica o do aparelho.
 */
export function quandoTermina(prazo: string, agora: string, fuso?: string): string {
  const opcoes = { timeZone: fuso } as const;
  const hora = new Date(prazo).toLocaleTimeString("pt-BR", { ...opcoes, hour: "2-digit", minute: "2-digit" });
  const dia = (iso: string) => new Date(iso).toLocaleDateString("pt-BR", { ...opcoes, day: "2-digit", month: "2-digit" });
  return dia(prazo) === dia(agora) ? `termina às ${hora}` : `termina em ${dia(prazo)} às ${hora}`;
}
