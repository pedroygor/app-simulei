import type { Desempenho } from "@/lib/api/desempenho";
import { CartaoDePontoFraco } from "./cartao-de-ponto-fraco";

/**
 * Por onde começar (4b). Linguagem neutra — "menor acerto", não "você é fraco em":
 * quem acerta 85% em tudo também tem os seus 3 menores. Sem assunto medido ainda,
 * diz o que falta em vez de sumir.
 */
export function PontosFracos({ desempenho }: { desempenho: Desempenho }) {
  return (
    <section aria-labelledby="menor-acerto" className="grid gap-3">
      <div>
        <h2 id="menor-acerto" className="font-heading text-lg font-bold">
          Seus assuntos com menor acerto
        </h2>
        <p className="text-muted-foreground text-sm">
          {desempenho.pontosFracos.length > 0
            ? "Comece o treino por aqui."
            : `Um assunto entra aqui quando você tiver respondido ${desempenho.amostraMinima} questões dele.`}
        </p>
      </div>
      {desempenho.pontosFracos.length > 0 && (
        <ul className="grid gap-3">
          {desempenho.pontosFracos.map((ponto) => (
            <CartaoDePontoFraco key={ponto.assunto.slug} ponto={ponto} />
          ))}
        </ul>
      )}
    </section>
  );
}
