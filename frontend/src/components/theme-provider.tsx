"use client";

import { ThemeProvider as NextThemesProvider } from "next-themes";
import type { ComponentProps } from "react";

/**
 * Envolve o next-themes. Precisa ser client component: o provider usa
 * localStorage e matchMedia, que não existem no servidor.
 *
 * O next-themes injeta um script inline ANTES da primeira pintura, o que
 * evita o flash de tema errado ao carregar (o famoso FOUC). Fazer isso na mão
 * daria trabalho e erraria em algum caso.
 *
 * `type` do script: no SERVIDOR vai sem type (JavaScript), e é esse que roda antes
 * da pintura. No CLIENTE vai como bloco de dados (`application/json`): quando o
 * React renderiza o layout de novo no cliente (o `notFound()` faz isso), ele não
 * executa script nenhum e, com type JavaScript, avisava no console "Encountered a
 * script tag while rendering React component". Bloco de dados não dispara o aviso
 * (`isScriptDataBlock` no react-dom), e não se perde nada: no cliente ele nunca
 * rodaria. A diferença de atributo não vira erro de hidratação porque o next-themes
 * já põe `suppressHydrationWarning` nessa tag.
 */
const scriptDoTema = { type: typeof window === "undefined" ? undefined : "application/json" };

export function ThemeProvider({ children, ...props }: ComponentProps<typeof NextThemesProvider>) {
  return (
    <NextThemesProvider scriptProps={scriptDoTema} {...props}>
      {children}
    </NextThemesProvider>
  );
}
