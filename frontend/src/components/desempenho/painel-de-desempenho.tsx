"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { buscarDesempenho } from "@/lib/api/desempenho";
import { agruparPorArea } from "@/lib/desempenho/desempenho";
import { AreaDoDesempenho } from "./area-do-desempenho";
import { PontosFracos } from "./pontos-fracos";
import { ResumoDoDesempenho } from "./resumo-do-desempenho";

/**
 * Onde o aluno acerta e erra, somando todas as provas entregues (4a), e por onde
 * começar a treinar (4b): os assuntos com menor acerto vêm logo depois do geral.
 *
 * Sem staleTime: entregar uma prova muda estes números, e voltar pra cá deve
 * mostrar isso.
 */
export function PainelDeDesempenho() {
  const consulta = useQuery({ queryKey: ["desempenho"], queryFn: () => buscarDesempenho() });

  if (consulta.isPending) return <EsqueletoDoPainel />;
  if (consulta.error) {
    return (
      <p role="alert" className="text-destructive py-8 text-center text-sm">
        {consulta.error.message}
      </p>
    );
  }

  const desempenho = consulta.data;
  if (desempenho.provas === 0) return <SemProvaEntregue />;

  return (
    <div className="grid gap-8 pb-8">
      <ResumoDoDesempenho desempenho={desempenho} />
      <PontosFracos desempenho={desempenho} />
      {agruparPorArea(desempenho).map((area) => (
        <AreaDoDesempenho key={area.area} area={area} amostraMinima={desempenho.amostraMinima} />
      ))}
    </div>
  );
}

function SemProvaEntregue() {
  return (
    <div className="grid gap-3 py-8 text-center">
      <p className="text-muted-foreground text-sm">
        Entregue uma prova para ver onde você acerta e onde erra, por área e por assunto.
      </p>
      <Button asChild size="xl">
        <Link href="/simulados/novo">Montar prova</Link>
      </Button>
    </div>
  );
}

function EsqueletoDoPainel() {
  return (
    <div className="grid gap-4" aria-hidden>
      <Skeleton className="h-32 w-full rounded-lg" />
      {Array.from({ length: 3 }).map((_, i) => (
        <Skeleton key={i} className="h-16 w-full rounded-lg" />
      ))}
    </div>
  );
}
