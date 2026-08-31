"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { ErroDaApi } from "@/lib/api/erros";

export function QueryProvider({ children }: { children: React.ReactNode }) {
  // useState com inicializador: cria UM cliente por montagem. Criar direto no
  // corpo do componente faria um cliente novo a cada render, jogando o cache
  // fora; criar no módulo compartilharia cache entre usuários no SSR.
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Erro 4xx é resposta definitiva do servidor: repetir não muda
            // nada e ainda atrasa a mensagem pro usuário. Só 5xx e rede valem
            // nova tentativa.
            retry: (tentativas, erro) => {
              if (erro instanceof ErroDaApi && erro.status < 500) return false;
              return tentativas < 2;
            },
            staleTime: 30_000,
            refetchOnWindowFocus: false,
          },
          mutations: {
            retry: false,
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
