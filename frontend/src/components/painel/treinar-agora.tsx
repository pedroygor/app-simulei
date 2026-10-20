import { CartaoDePontoFraco } from "@/components/desempenho/cartao-de-ponto-fraco";
import type { DesempenhoNoAssunto } from "@/lib/api/desempenho";

/** O assunto de menor acerto, com o "Treinar" da 4b: do painel ao treino num toque. */
export function TreinarAgora({ ponto }: { ponto: DesempenhoNoAssunto }) {
  return (
    <section aria-labelledby="treinar-agora" className="grid gap-3">
      <div>
        <h2 id="treinar-agora" className="font-heading text-lg font-bold">
          Treinar agora
        </h2>
        <p className="text-muted-foreground text-sm">O assunto em que você menos acerta.</p>
      </div>
      <ul>
        <CartaoDePontoFraco ponto={ponto} />
      </ul>
    </section>
  );
}
