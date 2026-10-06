import { requisitar } from "./cliente";
import type { Alternativa, Area, Idioma, Rotulo } from "./questoes";

/** Espelham o ResultadoResponse do backend. */

export type Placar = {
  questoes: number;
  respondidas: number;
  acertos: number;
  emBranco: number;
  /** null = nenhuma questão (não 0%: não houve o que errar). */
  percentual: number | null;
};

export type QuestaoCorrigida = {
  /** Posição na prova, a numeração que o aluno viu. */
  ordem: number;
  id: number;
  ano: number;
  numero: number;
  area: Area;
  idioma: Idioma | null;
  /** null = questão ainda não classificada. */
  materia: Rotulo | null;
  assunto: Rotulo | null;
  enunciado: string | null;
  comando: string | null;
  alternativas: Alternativa[];
  /** Id da alternativa marcada; null = em branco. */
  marcada: number | null;
  /** Letra do gabarito. */
  gabarito: string;
};

export type Resultado = {
  tentativaId: string;
  simuladoId: string;
  titulo: string;
  finalizadoEm: string;
  total: Placar;
  /** Na ordem fixa do ENEM. */
  porArea: { area: Area; nome: string; placar: Placar }[];
  /** Do pior aproveitamento pro melhor. */
  porAssunto: { area: Area; materia: Rotulo | null; assunto: Rotulo | null; placar: Placar }[];
  questoes: QuestaoCorrigida[];
};

/** Só da prova entregue: em andamento o backend responde 422. */
export function buscarResultado(tentativaId: string): Promise<Resultado> {
  return requisitar<Resultado>(`/tentativas/${tentativaId}/resultado`);
}
