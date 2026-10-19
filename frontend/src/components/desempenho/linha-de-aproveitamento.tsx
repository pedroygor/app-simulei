import type { ReactNode } from "react";
import { BarraDeAcerto } from "@/components/resultado/linha-de-placar";
import type { Aproveitamento } from "@/lib/api/desempenho";

/** Um assunto JÁ MEDIDO (com a amostra mínima): "x de y" sobre as respondidas e a barra. */
export function LinhaDeAproveitamento({ rotulo, aproveitamento }: { rotulo: ReactNode; aproveitamento: Aproveitamento }) {
  return (
    <li className="grid gap-1.5">
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className="min-w-0 break-words">{rotulo}</span>
        <span className="shrink-0 tabular-nums">
          {aproveitamento.acertos} de {aproveitamento.respondidas}
        </span>
      </div>
      <BarraDeAcerto percentual={aproveitamento.percentual} />
    </li>
  );
}
