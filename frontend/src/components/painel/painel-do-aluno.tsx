"use client";

import { useQuery } from "@tanstack/react-query";
import { Skeleton } from "@/components/ui/skeleton";
import { buscarDesempenho, buscarEvolucao } from "@/lib/api/desempenho";
import { buscarProvasEmAndamento } from "@/lib/api/tentativas";
import { AcoesDoPainel } from "./acoes-do-painel";
import { ComecoGuiado } from "./comeco-guiado";
import { ContinuarProvas } from "./continuar-provas";
import { ResumoDoPainel } from "./resumo-do-painel";
import { TreinarAgora } from "./treinar-agora";

/**
 * O painel (4d): "o que eu faço agora?". Continuar o que está aberto, ver como está,
 * treinar o assunto mais fraco, ou montar outra prova.
 *
 * Três consultas, nas MESMAS chaves de cache da /desempenho: quem vai do painel pro
 * desempenho (ou volta) não busca de novo. Nenhum endpoint agregador — as regras de
 * desempenho ficam num lugar só, no backend que já existe.
 */
export function PainelDoAluno() {
  const desempenho = useQuery({ queryKey: ["desempenho"], queryFn: () => buscarDesempenho() });
  const evolucao = useQuery({ queryKey: ["desempenho", "evolucao"], queryFn: () => buscarEvolucao() });
  const abertas = useQuery({ queryKey: ["tentativas", "em-andamento"], queryFn: () => buscarProvasEmAndamento() });

  if (desempenho.isPending || abertas.isPending) return <EsqueletoDoPainel />;
  if (desempenho.error || abertas.error) {
    return (
      <p role="alert" className="text-destructive py-8 text-center text-sm">
        {(desempenho.error ?? abertas.error)?.message}
      </p>
    );
  }

  const provasAbertas = abertas.data.provas;
  if (desempenho.data.provas === 0 && provasAbertas.length === 0) return <ComecoGuiado />;

  const maisFraco = desempenho.data.pontosFracos[0];
  return (
    <div className="grid gap-8 pb-8">
      {provasAbertas.length > 0 && <ContinuarProvas abertas={abertas.data} />}
      {desempenho.data.provas > 0 && <ResumoDoPainel desempenho={desempenho.data} evolucao={evolucao.data} />}
      {maisFraco && <TreinarAgora ponto={maisFraco} />}
      <AcoesDoPainel />
    </div>
  );
}

function EsqueletoDoPainel() {
  return (
    <div className="grid gap-4" aria-hidden>
      <Skeleton className="h-24 w-full rounded-lg" />
      <Skeleton className="h-36 w-full rounded-lg" />
      <Skeleton className="h-12 w-full rounded-lg" />
    </div>
  );
}
