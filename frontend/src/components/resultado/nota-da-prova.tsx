import type { Placar } from "@/lib/api/resultados";

/**
 * A nota em destaque: "3 de 5". O percentual vem do backend (arredondado pra
 * baixo) e é null quando a prova ficou sem questões.
 */
export function NotaDaProva({ placar }: { placar: Placar }) {
  return (
    <section aria-labelledby="nota" className="grid gap-1 rounded-lg border p-4">
      <h2 id="nota" className="text-muted-foreground text-sm">
        Sua nota
      </h2>
      <p className="font-heading text-4xl font-bold tabular-nums">
        {placar.acertos} <span className="text-muted-foreground text-2xl font-normal">de {placar.questoes}</span>
      </p>
      <p className="text-muted-foreground text-sm tabular-nums">
        {placar.percentual !== null && <>{placar.percentual}% de acerto</>}
        {placar.emBranco > 0 && <> · {placar.emBranco} em branco</>}
      </p>
    </section>
  );
}
