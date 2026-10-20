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
  /** Acerto até 30% com amostra suficiente: adivinhando, não sabendo (o chute dá 20%). */
  pertoDoChute: boolean;
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
  /** Até 3 assuntos MEDIDOS, do menor acerto pro maior. */
  pontosFracos: DesempenhoNoAssunto[];
};

/** O desempenho de quem está logado: o backend tira o aluno do token. */
export function buscarDesempenho(): Promise<Desempenho> {
  return requisitar<Desempenho>("/desempenho");
}

export type SemanaDaEvolucao = {
  /** A segunda-feira da semana (fuso de Brasília), "2026-10-13". */
  inicio: string;
  /** percentual null = semana sem prova entregue: um buraco no gráfico, nunca 0%. */
  geral: Aproveitamento;
  porArea: DesempenhoNaArea[];
};

export type Evolucao = { amostraMinima: number; semanas: SemanaDaEvolucao[] };

/** Toda semana da janela, em ordem, inclusive as vazias. Sem `semanas`, o backend usa 4. */
export function buscarEvolucao(semanas?: number): Promise<Evolucao> {
  return requisitar<Evolucao>(semanas ? `/desempenho/evolucao?semanas=${semanas}` : "/desempenho/evolucao");
}
