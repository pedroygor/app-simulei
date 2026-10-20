import { Dices } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import type { DesempenhoNoAssunto } from "@/lib/api/desempenho";
import { BotaoDeTreino } from "./botao-de-treino";

/**
 * Um dos assuntos com menor acerto. "Perto do chute" vai em TEXTO (o ícone e a cor
 * só reforçam): com até 30% de acerto, o aluno está adivinhando, não sabendo.
 */
export function CartaoDePontoFraco({ ponto }: { ponto: DesempenhoNoAssunto }) {
  const { aproveitamento } = ponto;
  return (
    <li className="grid gap-3 rounded-lg border p-4">
      <div className="grid gap-1">
        <div className="flex items-baseline justify-between gap-3">
          <h3 className="min-w-0 font-medium break-words">{ponto.assunto.nome}</h3>
          <span className="font-heading shrink-0 text-lg font-bold tabular-nums">{aproveitamento.percentual}%</span>
        </div>
        <p className="text-muted-foreground text-sm tabular-nums">
          {ponto.materia.nome} · {aproveitamento.acertos} de {aproveitamento.respondidas} certas
        </p>
        {aproveitamento.pertoDoChute && (
          <Badge variant="destructive">
            <Dices aria-hidden data-icon="inline-start" />
            Perto do chute
          </Badge>
        )}
      </div>
      <BotaoDeTreino assunto={ponto.assunto} />
    </li>
  );
}
