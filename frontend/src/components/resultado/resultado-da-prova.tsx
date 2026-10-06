"use client";

import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { buscarResultado, type Resultado } from "@/lib/api/resultados";
import { situacaoNaRevisao } from "@/lib/prova/correcao";
import { MapaDoResultado } from "./mapa-do-resultado";
import { NotaDaProva } from "./nota-da-prova";
import { PlacarPorArea } from "./placar-por-area";
import { PlacarPorAssunto } from "./placar-por-assunto";
import { RevisaoDaQuestao } from "./revisao-da-questao";

const HORA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

/**
 * A prova entregue: o resumo (nota, por área, por assunto, mapa) ou a revisão de
 * uma questão. O resultado não muda depois de entregue, exceto se o INEP anular
 * uma questão — raro o bastante pra não valer rebuscar a cada foco na aba.
 */
export function ResultadoDaProva({ tentativaId }: { tentativaId: string }) {
  const { data: resultado, isPending, error } = useQuery({
    queryKey: ["tentativas", tentativaId, "resultado"],
    queryFn: () => buscarResultado(tentativaId),
    staleTime: Infinity,
  });
  // null = resumo; número = índice da questão em revisão
  const [revisando, setRevisando] = useState<number | null>(null);

  if (isPending) return <EsqueletoDoResultado />;
  if (error) {
    return (
      <p role="alert" className="text-destructive py-8 text-center text-sm">
        {error.message}
      </p>
    );
  }

  if (revisando !== null) {
    return <Revisao resultado={resultado} indice={revisando} aoIr={setRevisando} />;
  }

  return (
    <div className="grid gap-8 pb-8">
      <header className="grid gap-1">
        <h1 className="font-heading text-2xl font-bold">Resultado</h1>
        <p className="text-sm">
          <span className="font-medium">{resultado.titulo}</span>
          <span className="text-muted-foreground"> · entregue em {HORA.format(new Date(resultado.finalizadoEm))}</span>
        </p>
      </header>

      <NotaDaProva placar={resultado.total} />
      <MapaDoResultado
        questoes={resultado.questoes.map((q) => ({ ordem: q.ordem, situacao: situacaoNaRevisao(q) }))}
        aoAbrir={setRevisando}
      />
      <PlacarPorArea areas={resultado.porArea} />
      <PlacarPorAssunto assuntos={resultado.porAssunto} />

      <div className="grid gap-2 sm:grid-cols-2">
        <Button asChild size="xl">
          <Link href="/simulados/novo">Montar outra prova</Link>
        </Button>
        <Button asChild variant="outline" size="xl">
          <Link href="/inicio">Voltar ao início</Link>
        </Button>
      </div>
    </div>
  );
}

/** Uma questão corrigida por tela, com o mesmo rodapé da prova. */
function Revisao({
  resultado,
  indice,
  aoIr,
}: {
  resultado: Resultado;
  indice: number;
  aoIr: (indice: number | null) => void;
}) {
  const total = resultado.questoes.length;
  const questao = resultado.questoes[indice];

  return (
    <div className="grid gap-6">
      <Button type="button" variant="ghost" className="justify-self-start" onClick={() => aoIr(null)}>
        <ChevronLeft aria-hidden />
        Resultado
      </Button>

      <RevisaoDaQuestao key={questao.id} questao={questao} total={total} />

      <nav aria-label="Navegação da revisão" className="bg-background/95 sticky bottom-0 -mx-4 border-t px-4 py-3 backdrop-blur">
        <div className="grid grid-cols-2 gap-2">
          <Button type="button" variant="outline" size="xl" disabled={indice === 0} onClick={() => aoIr(indice - 1)}>
            <ChevronLeft aria-hidden />
            Anterior
          </Button>
          {indice === total - 1 ? (
            <Button type="button" size="xl" onClick={() => aoIr(null)}>
              Ver resultado
            </Button>
          ) : (
            <Button type="button" size="xl" onClick={() => aoIr(indice + 1)}>
              Próxima
              <ChevronRight aria-hidden />
            </Button>
          )}
        </div>
      </nav>
    </div>
  );
}

function EsqueletoDoResultado() {
  return (
    <div className="space-y-6" aria-hidden>
      <Skeleton className="h-8 w-1/2" />
      <Skeleton className="h-28 w-full rounded-lg" />
      <Skeleton className="h-24 w-full rounded-lg" />
    </div>
  );
}
