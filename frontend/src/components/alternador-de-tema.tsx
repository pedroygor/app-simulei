"use client";

import { Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { Button } from "@/components/ui/button";

/**
 * Alterna claro/escuro. O professor projeta o painel no datashow e imprime
 * relatório — por isso o tema claro não é enfeite, é requisito.
 *
 * Os dois ícones são renderizados e a classe `dark` do <html> decide qual
 * aparece. Isso evita o padrão "useState + useEffect pra saber se montou":
 * o servidor não conhece o tema (vem de localStorage), então qualquer ícone
 * escolhido em JS no primeiro render daria divergência de hidratação.
 * Deixando o CSS decidir, o problema não existe.
 */
export function AlternadorDeTema() {
  const { resolvedTheme, setTheme } = useTheme();

  return (
    <Button
      variant="outline"
      size="icon"
      onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
      // Genérico de propósito: o label descreve a AÇÃO, que é sempre a mesma.
      // Um label que dependesse do tema atual voltaria ao problema acima.
      aria-label="Alternar entre tema claro e escuro"
    >
      <Moon className="dark:hidden" aria-hidden />
      <Sun className="hidden dark:block" aria-hidden />
    </Button>
  );
}
