import Link from "next/link";
import { Minus, TrendingDown, TrendingUp } from "lucide-react";
import type { Desempenho, Evolucao } from "@/lib/api/desempenho";
import { tendenciaSemanal } from "@/lib/painel/painel";

/**
 * O número geral (o mesmo da /desempenho) e a tendência da semana, que só aparece
 * com amostra nas duas semanas. A direção vai em TEXTO ("subiu", "caiu"); o ícone
 * só reforça — nada de verde/vermelho como sinal único.
 */
export function ResumoDoPainel({ desempenho, evolucao }: { desempenho: Desempenho; evolucao: Evolucao | undefined }) {
  const { geral } = desempenho;
  const tendencia = evolucao ? tendenciaSemanal(evolucao) : null;
  return (
    <section aria-labelledby="seu-resumo" className="grid gap-2 rounded-lg border p-4">
      <h2 id="seu-resumo" className="text-muted-foreground text-sm">
        Seu acerto, em {desempenho.provas === 1 ? "1 prova entregue" : `${desempenho.provas} provas entregues`}
      </h2>
      {geral.percentual !== null && <p className="font-heading text-4xl font-bold tabular-nums">{geral.percentual}%</p>}
      <p className="text-sm tabular-nums">
        Você acertou {geral.acertos} das {geral.respondidas} questões que respondeu.
      </p>
      {tendencia && <LinhaDaTendencia {...tendencia} />}
      <Link href="/desempenho" className="text-sm underline decoration-1 underline-offset-4 hover:decoration-2">
        Ver desempenho completo
      </Link>
    </section>
  );
}

function LinhaDaTendencia({ atual, anterior, diferenca }: { atual: number; anterior: number; diferenca: number }) {
  const Icone = diferenca > 0 ? TrendingUp : diferenca < 0 ? TrendingDown : Minus;
  const direcao =
    diferenca > 0 ? `subiu ${diferenca} pontos` : diferenca < 0 ? `caiu ${-diferenca} pontos` : "ficou igual";
  return (
    <p className="text-muted-foreground flex items-center gap-1.5 text-sm tabular-nums">
      <Icone aria-hidden className="size-4 shrink-0" />
      Esta semana {atual}%, semana passada {anterior}%: {direcao}.
    </p>
  );
}
