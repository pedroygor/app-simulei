"use client";

import { useQuery } from "@tanstack/react-query";
import { Skeleton } from "@/components/ui/skeleton";
import { buscarDesempenho, buscarEvolucao } from "@/lib/api/desempenho";
import { ErroDaApi } from "@/lib/api/erros";
import { serieGeral } from "@/lib/desempenho/evolucao";
import { BotaoDeTreino } from "./botao-de-treino";
import { GraficoSemanal } from "./grafico-semanal";
import { ResumoDoAssunto } from "./resumo-do-assunto";
import { TabelaDaEvolucao } from "./tabela-da-evolucao";

/**
 * "Treinei, melhorei?" (4c): um assunto só — o aproveitamento somado, a evolução
 * por semana e o "Treinar" logo abaixo, fechando o ciclo diagnóstico → treino → ver
 * de novo. O total vem do mesmo `GET /desempenho` da tela anterior (cache do
 * TanStack): quem chega por lá não busca de novo.
 */
export function DesempenhoNoAssunto({ slug }: { slug: string }) {
  const evolucao = useQuery({
    queryKey: ["desempenho", "evolucao", slug],
    queryFn: () => buscarEvolucao({ assunto: slug }),
  });
  const desempenho = useQuery({ queryKey: ["desempenho"], queryFn: () => buscarDesempenho() });

  if (evolucao.isPending) return <Skeleton aria-hidden className="h-64 w-full rounded-lg" />;
  if (evolucao.error) {
    const naoExiste = evolucao.error instanceof ErroDaApi && evolucao.error.status === 404;
    return (
      <p role="alert" className="text-muted-foreground py-8 text-center text-sm">
        {naoExiste ? "Esse assunto não existe na taxonomia do Simulei." : evolucao.error.message}
      </p>
    );
  }

  const { assunto } = evolucao.data;
  if (!assunto) return null;
  const total = desempenho.data?.porAssunto.find((a) => a.assunto.slug === slug)?.aproveitamento;

  return (
    <div className="grid gap-8 pb-8">
      <header className="grid gap-1">
        <p className="text-muted-foreground text-sm">{assunto.materia.nome}</p>
        <h1 className="font-heading text-2xl font-bold tracking-tight">{assunto.assunto.nome}</h1>
      </header>
      <ResumoDoAssunto aproveitamento={total} amostraMinima={evolucao.data.amostraMinima} />
      <BotaoDeTreino assunto={assunto.assunto} />
      <section aria-labelledby="evolucao-do-assunto" className="grid gap-4">
        <h2 id="evolucao-do-assunto" className="font-heading text-lg font-bold">
          Por semana
        </h2>
        <GraficoSemanal titulo={assunto.assunto.nome} colunas={serieGeral(evolucao.data)} tamanho="grande" />
        <TabelaDaEvolucao colunas={serieGeral(evolucao.data)} />
      </section>
    </div>
  );
}
