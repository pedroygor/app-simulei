import { separarPorAmostra, type AreaComAssuntos } from "@/lib/desempenho/desempenho";
import { AssuntosSemAmostra } from "./assuntos-sem-amostra";
import { LinkDoAssunto } from "./link-do-assunto";
import { LinhaDeAproveitamento } from "./linha-de-aproveitamento";

/**
 * Uma área: o aproveitamento dela no título, os assuntos já medidos com barra e os
 * que ainda não têm amostra recolhidos embaixo. Ordem da taxonomia nos dois.
 */
export function AreaDoDesempenho({ area, amostraMinima }: { area: AreaComAssuntos; amostraMinima: number }) {
  const { aproveitamento } = area;
  const titulo = `area-${area.area}`;
  const { medidos, semAmostra } = separarPorAmostra(area.assuntos);
  return (
    <section aria-labelledby={titulo} className="grid gap-3">
      <div className="grid gap-0.5">
        <div className="flex items-baseline justify-between gap-3">
          <h2 id={titulo} className="font-heading text-lg font-bold">
            {area.nome}
          </h2>
          {aproveitamento.percentual !== null && (
            <span className="font-heading text-lg font-bold tabular-nums">{aproveitamento.percentual}%</span>
          )}
        </div>
        <p className="text-muted-foreground text-sm tabular-nums">
          {aproveitamento.acertos} de {aproveitamento.respondidas} certas
          {aproveitamento.emBranco > 0 && <> · {aproveitamento.emBranco} em branco</>}
        </p>
      </div>
      {medidos.length > 0 && (
        <ul className="grid gap-3">
          {medidos.map((a) => (
            <LinhaDeAproveitamento
              key={a.assunto.slug}
              rotulo={
                <>
                  <LinkDoAssunto assunto={a.assunto} />
                  <span className="text-muted-foreground"> · {a.materia.nome}</span>
                </>
              }
              aproveitamento={a.aproveitamento}
            />
          ))}
        </ul>
      )}
      <AssuntosSemAmostra assuntos={semAmostra} amostraMinima={amostraMinima} />
    </section>
  );
}
