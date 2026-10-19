import { requisitar } from "./cliente";
import type { Area, Rotulo } from "./questoes";

/** Espelham o DesempenhoResponse do backend. */

export type Aproveitamento = {
  respondidas: number;
  acertos: number;
  /** Sinal à parte: NÃO entra no percentual (em branco pode ser falta de tempo, não de saber). */
  emBranco: number;
  /** acertos / respondidas; null = nenhuma resposta (não 0%). */
  percentual: number | null;
  amostraSuficiente: boolean;
};

export type DesempenhoNaArea = { area: Area; nome: string; aproveitamento: Aproveitamento };
export type DesempenhoNoAssunto = { area: Area; materia: Rotulo; assunto: Rotulo; aproveitamento: Aproveitamento };

export type Desempenho = {
  /** Quantas provas entregues entraram na conta. */
  provas: number;
  /** Respostas que um assunto precisa pra virar diagnóstico. */
  amostraMinima: number;
  geral: Aproveitamento;
  /** Na ordem fixa do ENEM. */
  porArea: DesempenhoNaArea[];
  /** Na ordem da taxonomia. */
  porAssunto: DesempenhoNoAssunto[];
};

/** O desempenho de quem está logado: o backend tira o aluno do token. */
export function buscarDesempenho(): Promise<Desempenho> {
  return requisitar<Desempenho>("/desempenho");
}
