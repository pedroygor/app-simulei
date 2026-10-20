import { Dices } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import type { Aproveitamento } from "@/lib/api/desempenho";
import { faltamRespostas } from "@/lib/desempenho/desempenho";

/** O total do assunto em todas as provas entregues, com as mesmas regras da tela de desempenho. */
export function ResumoDoAssunto({
  aproveitamento,
  amostraMinima,
}: {
  aproveitamento: Aproveitamento | undefined;
  amostraMinima: number;
}) {
  if (!aproveitamento || aproveitamento.respondidas === 0) {
    return (
      <p className="text-muted-foreground rounded-lg border p-4 text-sm">
        Você ainda não respondeu questões deste assunto numa prova entregue.
      </p>
    );
  }
  const faltam = faltamRespostas(aproveitamento, amostraMinima);
  return (
    <section aria-label="Total no assunto" className="grid gap-1 rounded-lg border p-4">
      <p className="font-heading text-4xl font-bold tabular-nums">{aproveitamento.percentual}%</p>
      <p className="text-sm tabular-nums">
        Você acertou {aproveitamento.acertos} das {aproveitamento.respondidas} questões que respondeu.
      </p>
      {faltam > 0 && (
        <p className="text-muted-foreground text-sm">Responda mais {faltam} para medir este assunto com segurança.</p>
      )}
      {aproveitamento.pertoDoChute && (
        <Badge variant="destructive">
          <Dices aria-hidden data-icon="inline-start" />
          Perto do chute
        </Badge>
      )}
    </section>
  );
}
