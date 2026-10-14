import type { Pagina } from "@/lib/api/questoes";
import type { ProvaDoHistorico } from "@/lib/api/simulados";

/** A linha de números do cartão, conforme a situação da prova. */
export function resumoDaProva(prova: ProvaDoHistorico): string {
  if (prova.situacao === "ENTREGUE" && prova.placar) {
    const { acertos, questoes, percentual } = prova.placar;
    // percentual null = todas as questões foram retiradas depois (não é 0%)
    return percentual === null ? "Sem questões a corrigir" : `${acertos} de ${questoes} · ${percentual}%`;
  }
  if (prova.situacao === "EM_ANDAMENTO") return `${prova.respondidas} de ${prova.questoes} respondidas`;
  return prova.questoes === 1 ? "1 questão" : `${prova.questoes} questões`;
}

/**
 * Pra onde o cartão leva. Iniciada (aberta ou entregue): a tela da tentativa,
 * que já decide entre prova e resultado. Não iniciada: não há tentativa ainda —
 * quem toca precisa CRIÁ-LA (POST), então é uma ação, não um link.
 */
export function destinoDaProva(prova: ProvaDoHistorico): string | null {
  return prova.tentativaId ? `/tentativas/${prova.tentativaId}` : null;
}

/** Pro getNextPageParam do useInfiniteQuery: undefined = acabou. */
export function proximaPagina(ultima: Pagina<unknown>): number | undefined {
  const seguinte = ultima.pagina + 1;
  return seguinte < ultima.totalDePaginas ? seguinte : undefined;
}
