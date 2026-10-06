import type { Resultado } from "@/lib/api/resultados";
import { LinhaDePlacar } from "./linha-de-placar";

/**
 * Do assunto em que mais errou pro que mais acertou (a ordem vem do backend).
 * Só números, sem rótulo de "ponto fraco": 1 questão num assunto não diz nada —
 * o diagnóstico, com amostra mínima, é da Etapa 4.
 */
export function PlacarPorAssunto({ assuntos }: { assuntos: Resultado["porAssunto"] }) {
  return (
    <section aria-labelledby="por-assunto" className="grid gap-3">
      <div>
        <h2 id="por-assunto" className="font-heading text-lg font-bold">
          Por assunto
        </h2>
        <p className="text-muted-foreground text-sm">Do que você mais errou pro que mais acertou</p>
      </div>
      <ul className="grid gap-3">
        {assuntos.map((a) => (
          <LinhaDePlacar
            key={a.assunto?.slug ?? `sem-assunto-${a.area}`}
            rotulo={<RotuloDoAssunto materia={a.materia?.nome} assunto={a.assunto?.nome} />}
            placar={a.placar}
          />
        ))}
      </ul>
    </section>
  );
}

function RotuloDoAssunto({ materia, assunto }: { materia?: string; assunto?: string }) {
  if (!assunto) return <span className="text-muted-foreground">Ainda sem assunto</span>;
  return (
    <>
      {assunto}
      {materia && <span className="text-muted-foreground"> · {materia}</span>}
    </>
  );
}
