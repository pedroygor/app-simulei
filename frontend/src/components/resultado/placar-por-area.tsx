import type { Resultado } from "@/lib/api/resultados";
import { LinhaDePlacar } from "./linha-de-placar";

export function PlacarPorArea({ areas }: { areas: Resultado["porArea"] }) {
  // Prova de uma área só: a linha repetiria a nota
  if (areas.length < 2) return null;

  return (
    <section aria-labelledby="por-area" className="grid gap-3">
      <h2 id="por-area" className="font-heading text-lg font-bold">
        Por área
      </h2>
      <ul className="grid gap-3">
        {areas.map((a) => (
          <LinhaDePlacar key={a.area} rotulo={a.nome} placar={a.placar} />
        ))}
      </ul>
    </section>
  );
}
