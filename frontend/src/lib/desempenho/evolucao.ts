import type { Area } from "@/lib/api/questoes";
import type { Aproveitamento, Evolucao } from "@/lib/api/desempenho";

/** Uma coluna do gráfico: a semana e o aproveitamento nela (null = sem prova entregue). */
export type Coluna = { inicio: string; aproveitamento: Aproveitamento | null };

export type SerieDaArea = { area: Area; nome: string; colunas: Coluna[] };

/**
 * "2026-10-13" → "13/10". Trata a data como TEXTO de propósito: `new Date("2026-10-13")`
 * é meia-noite em UTC, que no Brasil ainda é dia 12 — o rótulo sairia um dia antes.
 */
export function rotuloDaSemana(inicio: string): string {
  const [, mes, dia] = inicio.split("-");
  return `${dia}/${mes}`;
}

/** A série geral: uma coluna por semana; semana sem resposta vira coluna vazia. */
export function serieGeral(evolucao: Evolucao): Coluna[] {
  return evolucao.semanas.map((s) => ({
    inicio: s.inicio,
    aproveitamento: s.geral.respondidas > 0 ? s.geral : null,
  }));
}

/**
 * Uma série por área que teve resposta em ALGUMA semana da janela, na ordem em que as
 * áreas aparecem (a do ENEM, que o backend já entrega). Área sem nada na janela não
 * ganha gráfico: seriam quatro colunas vazias.
 */
export function seriesPorArea(evolucao: Evolucao): SerieDaArea[] {
  const nomes = new Map<Area, string>();
  for (const semana of evolucao.semanas) {
    for (const a of semana.porArea) if (a.aproveitamento.respondidas > 0) nomes.set(a.area, a.nome);
  }
  const ordem = ["LINGUAGENS", "CIENCIAS_HUMANAS", "CIENCIAS_NATUREZA", "MATEMATICA"] as const;
  return ordem
    .filter((area) => nomes.has(area))
    .map((area) => ({
      area,
      nome: nomes.get(area)!,
      colunas: evolucao.semanas.map((s) => {
        const naArea = s.porArea.find((a) => a.area === area)?.aproveitamento;
        return { inicio: s.inicio, aproveitamento: naArea && naArea.respondidas > 0 ? naArea : null };
      }),
    }));
}

/** Índice da última coluna com resposta (a que a leitura mostra de saída); null se nenhuma. */
export function ultimaComDado(colunas: Coluna[]): number | null {
  for (let i = colunas.length - 1; i >= 0; i--) if (colunas[i].aproveitamento) return i;
  return null;
}

/** A frase da leitura de uma coluna: o que o leitor de tela ouve e o que aparece acima do gráfico. */
export function leituraDaColuna(coluna: Coluna): string {
  const semana = `Semana de ${rotuloDaSemana(coluna.inicio)}`;
  const a = coluna.aproveitamento;
  if (!a) return `${semana}: sem prova entregue`;
  const base = `${semana}: ${a.percentual}% · ${a.acertos} de ${a.respondidas} certas`;
  return a.amostraSuficiente ? base : `${base} (poucas respostas)`;
}
