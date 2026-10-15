/** Espelham os DTOs de resposta do backend. */

// Um papel só desde 15/10/2026 (o professor saiu do produto); o tipo fica
// porque o backend continua mandando o campo
export type Role = "ALUNO";
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
