import { requisitar } from "./cliente";
import { guardarAccessToken, limparAccessToken } from "./token";
import type { RespostaDeToken, Role, Usuario } from "./tipos";

export type DadosDeLogin = {
  email: string;
  senha: string;
};

export type DadosDeCadastro = {
  nome: string;
  email: string;
  senha: string;
  role: Role;
};

export async function entrar(dados: DadosDeLogin): Promise<Usuario> {
  const { accessToken } = await requisitar<RespostaDeToken>("/auth/login", {
    method: "POST",
    body: dados,
  });
  // O refresh token vem junto, em cookie httpOnly — o JS não o vê nem precisa
  guardarAccessToken(accessToken);
  return buscarMeuPerfil();
}

export async function cadastrar(dados: DadosDeCadastro): Promise<Usuario> {
  return requisitar<Usuario>("/usuarios", { method: "POST", body: dados });
}

export function buscarMeuPerfil(): Promise<Usuario> {
  return requisitar<Usuario>("/usuarios/me");
}

/**
 * Tenta recuperar a sessão usando só o cookie de refresh.
 *
 * É o que faz o F5 não deslogar: o access token mora em memória e morre no
 * reload, mas o cookie sobrevive. Devolve null quando não há sessão — este é
 * o único lugar onde "sem sessão" é resultado normal, não erro.
 */
export async function recuperarSessao(): Promise<Usuario | null> {
  try {
    const { accessToken } = await requisitar<RespostaDeToken>("/auth/refresh", { method: "POST" });
    guardarAccessToken(accessToken);
    return await buscarMeuPerfil();
  } catch {
    limparAccessToken();
    return null;
  }
}

export async function sair(): Promise<void> {
  try {
    // Expira o cookie no navegador. Importante no computador compartilhado:
    // sem isso o próximo usuário retomaria a sessão com um refresh.
    await requisitar<void>("/auth/logout", { method: "POST" });
  } finally {
    // Mesmo se a chamada falhar, o token local tem que sumir
    limparAccessToken();
  }
}
