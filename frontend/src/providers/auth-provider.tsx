"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContext, use, useMemo } from "react";
import {
  cadastrar as cadastrarNaApi,
  entrar as entrarNaApi,
  recuperarSessao,
  sair as sairDaApi,
  type DadosDeCadastro,
  type DadosDeLogin,
} from "@/lib/api/auth";
import type { Usuario } from "@/lib/api/tipos";

export const CHAVE_DA_SESSAO = ["sessao"] as const;

type ContextoDeAutenticacao = {
  usuario: Usuario | null;
  /** true enquanto a tentativa de recuperar a sessão não terminou. */
  carregando: boolean;
  entrar: (dados: DadosDeLogin) => Promise<Usuario>;
  cadastrar: (dados: DadosDeCadastro) => Promise<Usuario>;
  sair: () => Promise<void>;
};

const Contexto = createContext<ContextoDeAutenticacao | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();

  /**
   * Na montagem, tenta trocar o cookie de refresh por um access token novo.
   *
   * É isto que faz o F5 não deslogar: o token mora em memória e some no
   * reload, mas o cookie httpOnly sobrevive. Enquanto esta consulta não
   * responde, não dá pra saber se o usuário está logado — daí o `carregando`.
   */
  const { data: usuario, isPending } = useQuery({
    queryKey: CHAVE_DA_SESSAO,
    queryFn: recuperarSessao,
    // Sem sessão é resposta normal (null), não erro: não faz sentido repetir
    retry: false,
    staleTime: Infinity,
  });

  const mutacaoDeLogin = useMutation({
    mutationFn: entrarNaApi,
    onSuccess: (usuarioLogado) => queryClient.setQueryData(CHAVE_DA_SESSAO, usuarioLogado),
  });

  const mutacaoDeCadastro = useMutation({ mutationFn: cadastrarNaApi });

  const mutacaoDeSaida = useMutation({
    mutationFn: sairDaApi,
    onSuccess: () => {
      queryClient.setQueryData(CHAVE_DA_SESSAO, null);
      // Limpa TODO o cache: dados do usuário anterior não podem vazar pro
      // próximo — de novo, o computador compartilhado do cursinho
      queryClient.clear();
    },
  });

  const valor = useMemo<ContextoDeAutenticacao>(
    () => ({
      usuario: usuario ?? null,
      carregando: isPending,
      entrar: mutacaoDeLogin.mutateAsync,
      cadastrar: mutacaoDeCadastro.mutateAsync,
      sair: mutacaoDeSaida.mutateAsync,
    }),
    [
      usuario,
      isPending,
      mutacaoDeLogin.mutateAsync,
      mutacaoDeCadastro.mutateAsync,
      mutacaoDeSaida.mutateAsync,
    ],
  );

  return <Contexto value={valor}>{children}</Contexto>;
}

export function useSessao() {
  const contexto = use(Contexto);
  if (!contexto) {
    throw new Error("useSessao precisa estar dentro de <AuthProvider>");
  }
  return contexto;
}
