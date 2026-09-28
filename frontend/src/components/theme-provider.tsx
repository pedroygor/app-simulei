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
 */
export function ThemeProvider({ children, ...props }: ComponentProps<typeof NextThemesProvider>) {
  return <NextThemesProvider {...props}>{children}</NextThemesProvider>;
}
