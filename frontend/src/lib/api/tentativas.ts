import { requisitar } from "./cliente";
import type { Alternativa, Area, Idioma } from "./questoes";

/** Espelham os DTOs de TentativaController no backend. */

export type StatusDaTentativa = "EM_ANDAMENTO" | "FINALIZADA";

export type Tentativa = {
  id: string;
  simuladoId: string;
  status: StatusDaTentativa;
  iniciadoEm: string;
  finalizadoEm: string | null;
};

export type QuestaoDaProva = {
  /** Posição na prova (1, 2, 3...), não o número no caderno do ENEM. */
  ordem: number;
  id: number;
  ano: number;
  numero: number;
  area: Area;
  idioma: Idioma | null;
  enunciado: string | null;
  comando: string | null;
  alternativas: Alternativa[];
  /** Id da alternativa marcada; null = em branco. */
  marcada: number | null;
};

/** Sem gabarito, correção, assunto nem habilidade: ver ProvaResponse no backend. */
export type Prova = {
  tentativaId: string;
  simuladoId: string;
  titulo: string;
  /** null = sem cronômetro. */
  tempoLimiteSeg: number | null;
  status: StatusDaTentativa;
  iniciadoEm: string;
  /** Até quando dá pra responder; null = sem cronômetro. O servidor recusa depois. */
  prazo: string | null;
  /** Hora do servidor na resposta: base pra descontar o relógio do celular. */
  agora: string;
  finalizadoEm: string | null;
  questoes: QuestaoDaProva[];
};

/** Cria a tentativa ou devolve a que já existe (201 ou 200: o corpo é o mesmo). */
export function iniciarTentativa(simuladoId: string): Promise<Tentativa> {
  return requisitar<Tentativa>(`/simulados/${simuladoId}/tentativas`, { method: "POST" });
}

export function buscarProva(tentativaId: string): Promise<Prova> {
  return requisitar<Prova>(`/tentativas/${tentativaId}`);
}

export function responder(tentativaId: string, questaoId: number, alternativaId: number): Promise<void> {
  return requisitar<void>(`/tentativas/${tentativaId}/respostas/${questaoId}`, {
    method: "PUT",
    body: { alternativaId },
  });
}

export function desmarcar(tentativaId: string, questaoId: number): Promise<void> {
  return requisitar<void>(`/tentativas/${tentativaId}/respostas/${questaoId}`, { method: "DELETE" });
}

export function finalizarTentativa(tentativaId: string): Promise<Tentativa> {
  return requisitar<Tentativa>(`/tentativas/${tentativaId}/finalizar`, { method: "POST" });
}

/** Espelha o ProvasEmAndamentoResponse: até 3 provas abertas, as mais recentes primeiro. */
export type ProvaAberta = {
  tentativaId: string;
  simuladoId: string;
  titulo: string;
  iniciadoEm: string;
  /** null = sem cronômetro. */
  prazo: string | null;
  questoes: number;
  respondidas: number;
};

export type ProvasAbertas = { agora: string; provas: ProvaAberta[] };

export function buscarProvasEmAndamento(): Promise<ProvasAbertas> {
  return requisitar<ProvasAbertas>("/tentativas/em-andamento");
}
