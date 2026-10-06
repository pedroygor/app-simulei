import type { EstadoDaBolha } from "@/components/questao/alternativa-bolha";
import type { Alternativa } from "@/lib/api/questoes";
import type { QuestaoCorrigida } from "@/lib/api/resultados";

export type SituacaoNaRevisao = "acertou" | "errou" | "emBranco";

/**
 * O estado de cada bolha na revisão. A resposta do backend não traz `correta`:
 * é derivado aqui de `marcada` (id) × `gabarito` (letra) — uma regra só, testada.
 */
export function estadoNaRevisao(
  alternativa: Pick<Alternativa, "id" | "letra">,
  questao: Pick<QuestaoCorrigida, "marcada" | "gabarito">,
): EstadoDaBolha {
  const ehGabarito = alternativa.letra === questao.gabarito;
  const marcada = alternativa.id === questao.marcada;
  if (marcada) return ehGabarito ? "acertou" : "incorreta";
  return ehGabarito ? "correta" : "neutra";
}

/** Como a questão aparece no mapa de bolhas do resultado. */
export function situacaoNaRevisao(
  questao: Pick<QuestaoCorrigida, "marcada" | "gabarito"> & { alternativas: Pick<Alternativa, "id" | "letra">[] },
): SituacaoNaRevisao {
  if (questao.marcada === null) return "emBranco";
  const marcada = questao.alternativas.find((a) => a.id === questao.marcada);
  return marcada?.letra === questao.gabarito ? "acertou" : "errou";
}
