"use client";

import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import type { FiltroDeQuestoes } from "@/lib/api/questoes";
import { montarUrlDeQuestoes } from "@/lib/questoes/filtro-da-url";
import { cn } from "@/lib/utils";

type Props = {
  filtro: FiltroDeQuestoes;
  /** Zero-based, como vem do Spring Data. */
  paginaAtual: number;
  totalDePaginas: number;
  totalDeQuestoes: number;
};

export function Paginacao({ filtro, paginaAtual, totalDePaginas, totalDeQuestoes }: Props) {
  // Fora do intervalo a lista já mostra "esta página não existe" com a saída;
  // repetir a barra aqui anunciaria "100 de 3"
  if (totalDePaginas <= 1 || paginaAtual > totalDePaginas - 1) return null;

  const temAnterior = paginaAtual > 0;
  const temProxima = paginaAtual < totalDePaginas - 1;

  return (
    <nav
      // Sem rótulo, o leitor de tela anuncia só "navegação" — e a página pode
      // ter mais de uma
      aria-label="Paginação das questões"
      className="flex items-center justify-between gap-3 border-t pt-4"
    >
      <Salto
        filtro={filtro}
        para={paginaAtual - 1}
        habilitado={temAnterior}
        rotulo="Página anterior"
      >
        <ChevronLeft aria-hidden className="size-4" />
        <span className="max-sm:sr-only">Anterior</span>
      </Salto>

      {/* aria-live: trocar de página não recarrega a tela inteira, então sem
          isto o leitor de tela não anunciaria que a posição mudou */}
      <p role="status" aria-live="polite" className="text-muted-foreground text-sm tabular-nums">
        {paginaAtual + 1} de {totalDePaginas}
        <span className="sr-only"> — {totalDeQuestoes} questões no total</span>
      </p>

      <Salto filtro={filtro} para={paginaAtual + 1} habilitado={temProxima} rotulo="Próxima página">
        <span className="max-sm:sr-only">Próxima</span>
        <ChevronRight aria-hidden className="size-4" />
      </Salto>
    </nav>
  );
}

function Salto({
  filtro,
  para,
  habilitado,
  rotulo,
  children,
}: {
  filtro: FiltroDeQuestoes;
  para: number;
  habilitado: boolean;
  rotulo: string;
  children: React.ReactNode;
}) {
  const classe = cn(buttonVariants({ variant: "outline", size: "sm" }), "gap-1");

  // Na ponta do intervalo vira <span>, não <a> desabilitado: link que não leva
  // a lugar nenhum ainda recebe foco e confunde quem navega por teclado
  if (!habilitado) {
    return (
      <span aria-hidden className={cn(classe, "pointer-events-none opacity-50")}>
        {children}
      </span>
    );
  }

  return (
    <Link href={montarUrlDeQuestoes({ ...filtro, pagina: para })} aria-label={rotulo} className={classe}>
      {children}
    </Link>
  );
}
