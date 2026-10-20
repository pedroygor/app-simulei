import type { Rotulo } from "@/lib/api/questoes";
import type { PedidoDeSimulado } from "@/lib/api/simulados";
import type { Taxonomia } from "@/lib/api/taxonomia";

/** Um treino curto: dá pra fazer numa sentada, no celular. */
export const QUESTOES_DO_TREINO = 10;

/** O mínimo que a montagem aceita (`@Min(5)` no CriarSimuladoRequest do backend). */
export const MINIMO_DA_PROVA = 5;

/** Quantas questões disponíveis o assunto tem no banco inteiro (0 se não achar). */
export function questoesNoBanco(taxonomia: Taxonomia, assunto: string): number {
  for (const area of taxonomia) {
    for (const materia of area.materias) {
      const achado = materia.assuntos.find((a) => a.slug === assunto);
      if (achado) return achado.questoes;
    }
  }
  return 0;
}

/**
 * O pedido de "treinar este assunto": até 10 questões, limitado ao que o banco tem,
 * sem cronômetro (treino não é simulado de prova). null quando o assunto tem menos
 * questões do que a montagem aceita — o botão nem aparece, em vez de levar um 422.
 * O sorteio já prefere as questões que o aluno ainda não viu.
 */
export function pedidoDeTreino(assunto: Rotulo, disponiveis: number): PedidoDeSimulado | null {
  if (disponiveis < MINIMO_DA_PROVA) return null;
  return {
    // O título tem teto de 120 no backend; os nomes de assunto cabem, mas sem confiar nisso
    titulo: `Treino: ${assunto.nome}`.slice(0, 120),
    total: Math.min(QUESTOES_DO_TREINO, disponiveis),
    anos: [],
    itens: [{ tipo: "ASSUNTO", valor: assunto.slug }],
  };
}
