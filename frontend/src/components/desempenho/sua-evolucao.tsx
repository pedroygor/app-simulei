"use client";

import { useQuery } from "@tanstack/react-query";
import { Skeleton } from "@/components/ui/skeleton";
import { buscarEvolucao } from "@/lib/api/desempenho";
import { serieGeral, seriesPorArea } from "@/lib/desempenho/evolucao";
import { GraficoSemanal } from "./grafico-semanal";
import { TabelaDaEvolucao } from "./tabela-da-evolucao";

/**
 * "Estou melhorando?" (4c): o acerto por semana nas últimas 4 semanas, geral e por
 * área. Consulta própria, separada do desempenho: carrega e falha sozinha, sem
 * derrubar o resto da tela.
 */
export function SuaEvolucao() {
  const consulta = useQuery({ queryKey: ["desempenho", "evolucao"], queryFn: () => buscarEvolucao() });

  if (consulta.isPending) return <Skeleton aria-hidden className="h-48 w-full rounded-lg" />;
  if (consulta.error) {
    return (
      <p role="alert" className="text-destructive text-sm">
        {consulta.error.message}
      </p>
    );
  }

  const geral = serieGeral(consulta.data);
  const areas = seriesPorArea(consulta.data);
  return (
    <section aria-labelledby="sua-evolucao" className="grid gap-4">
      <div>
        <h2 id="sua-evolucao" className="font-heading text-lg font-bold">
          Sua evolução
        </h2>
        <p className="text-muted-foreground text-sm">
          Acerto nas questões respondidas, por semana de entrega. Toque numa coluna para ver os números.
        </p>
      </div>
      <GraficoSemanal titulo="Geral" colunas={geral} tamanho="grande" />
      {/* Uma área só: o gráfico dela repetiria o geral */}
      {areas.length > 1 &&
        areas.map((serie) => (
          <GraficoSemanal key={serie.area} titulo={serie.nome} colunas={serie.colunas} tamanho="pequeno" />
        ))}
      <TabelaDaEvolucao colunas={geral} />
    </section>
  );
}
