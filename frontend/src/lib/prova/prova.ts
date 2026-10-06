import type { QuestaoDaProva } from "@/lib/api/tentativas";

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Lê o id do segmento `/tentativas/[id]`. Fora do formato vira 404 na própria
 * página, sem chamar a API (que responderia 400 pra um id que não é UUID).
 */
export function lerIdDaTentativa(texto: string): string | null {
  return UUID.test(texto) ? texto.toLowerCase() : null;
}

/**
 * Em que questão a prova abre: a primeira em branco. Quem fechou o navegador no
 * meio continua de onde parou, e não na questão 1 que já respondeu. Prova toda
 * respondida abre na primeira (revisão).
 */
export function indiceInicial(questoes: Pick<QuestaoDaProva, "marcada">[]): number {
  const indice = questoes.findIndex((q) => q.marcada === null);
  return indice === -1 ? 0 : indice;
}

/** Quantas questões estão em branco, considerando o que está marcado AGORA na tela. */
export function contarEmBranco(questoes: Pick<QuestaoDaProva, "id">[], marcadas: ReadonlyMap<number, number | null>) {
  return questoes.filter((q) => (marcadas.get(q.id) ?? null) === null).length;
}

/** O que o servidor já tinha marcado, no formato da fila de salvamento. */
export function marcadasIniciais(questoes: Pick<QuestaoDaProva, "id" | "marcada">[]): Map<number, number | null> {
  return new Map(questoes.map((q) => [q.id, q.marcada]));
}
