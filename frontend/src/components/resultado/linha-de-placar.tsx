import type { ReactNode } from "react";
import type { Placar } from "@/lib/api/resultados";

/**
 * Um rótulo com o "x de y" e uma barra. A barra é DECORATIVA (aria-hidden): o
 * número está sempre em texto ao lado, que é o que o leitor de tela lê e o que
 * não depende de enxergar comprimento.
 */
export function LinhaDePlacar({ rotulo, placar }: { rotulo: ReactNode; placar: Placar }) {
  return (
    <li className="grid gap-1.5">
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className="min-w-0 break-words">{rotulo}</span>
        <span className="shrink-0 tabular-nums">
          {placar.acertos} de {placar.questoes}
        </span>
      </div>
      <BarraDeAcerto percentual={placar.percentual} />
    </li>
  );
}

export function BarraDeAcerto({ percentual }: { percentual: number | null }) {
  return (
    <div aria-hidden className="bg-muted h-1.5 overflow-hidden rounded-full">
      {/* width inline: é DADO (o percentual), não estilo — não existe classe
          utilitária pra cada um dos 101 valores */}
      <div className="bg-primary h-full rounded-full" style={{ width: `${percentual ?? 0}%` }} />
    </div>
  );
}
