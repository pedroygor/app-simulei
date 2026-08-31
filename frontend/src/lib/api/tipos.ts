/** Espelham os DTOs de resposta do backend. */

export type Role = "ALUNO" | "PROFESSOR";
export type Plano = "FREE" | "PRO";

export type Usuario = {
  id: string;
  nome: string;
  email: string;
  role: Role;
  plano: Plano;
  criadoEm: string;
  ativo: boolean;
};

export type RespostaDeToken = {
  accessToken: string;
  tokenType: string;
  /** Segundos até expirar (1800 = 30 min). */
  expiresIn: number;
};
