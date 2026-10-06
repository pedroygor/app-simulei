"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { ErroDaApi } from "@/lib/api/erros";
import { buscarProva } from "@/lib/api/tentativas";
import { ProvaEmAndamento } from "./prova-em-andamento";
import { ProvaEntregue } from "./prova-entregue";

/**
 * Busca a prova e decide o que mostrar. Client Component pelo motivo de sempre:
 * o token vive em memória.
 */
export function TelaDaProva({ id }: { id: string }) {
  const chave = ["tentativas", id] as const;
  const { data: prova, isPending, error } = useQuery({
    queryKey: chave,
    queryFn: () => buscarProva(id),
    // A prova (questões e ordem) não muda; as marcações vivem na fila de
    // salvamento. Rebuscar ao voltar pra aba não traria nada novo
    staleTime: Infinity,
  });

  // Quantas questões sumiram desde que a prova foi aberta (anuladas no meio).
  // Sem avisar, o aluno veria OUTRA questão no mesmo número e acharia que a tela
  // trocou sozinho. Estado ajustado durante o render quando a lista muda — o
  // padrão do React pra "derivar do valor anterior", sem useEffect nem ref
  const [conhecidas, setConhecidas] = useState<number | null>(null);
  const [retiradas, setRetiradas] = useState(0);
  const total = prova?.questoes.length;
  if (total !== undefined && total !== conhecidas) {
    if (conhecidas !== null && total < conhecidas) setRetiradas((r) => r + conhecidas - total);
    setConhecidas(total);
  }

  if (isPending) return <EsqueletoDaProva />;

  if (error) {
    // Alheia e inexistente dão o mesmo 404 no backend
    const naoExiste = error instanceof ErroDaApi && error.status === 404;
    return (
      <div className="space-y-3 py-8 text-center text-sm">
        <p role="alert" className={naoExiste ? "text-muted-foreground" : "text-destructive"}>
          {naoExiste ? "Esta prova não existe ou não é sua." : error.message}
        </p>
        <Link href="/inicio" className="underline">
          Voltar ao início
        </Link>
      </div>
    );
  }

  if (prova.status === "FINALIZADA") return <ProvaEntregue prova={prova} />;

  // Questão anulada ou incompleta depois da montagem sai da prova (backend). Se
  // todas saíram, não há o que responder
  if (prova.questoes.length === 0) {
    return (
      <div className="space-y-3 py-8 text-center text-sm">
        <p role="alert" className="text-muted-foreground">
          As questões desta prova foram retiradas (anuladas pelo INEP ou sem o texto necessário) e ela não tem mais o que responder.
        </p>
        <Link href="/simulados/novo" className="underline">
          Montar outra prova
        </Link>
      </div>
    );
  }

  // key com os ids: se a prova encolher com ela aberta (uma questão anulada no
  // meio, que o 422 ao responder faz recarregar), a fila de salvamento e a
  // questão atual nascem de novo com a lista nova — o índice antigo poderia
  // apontar pra além do fim
  return (
    <ProvaEmAndamento
      key={prova.questoes.map((q) => q.id).join(",")}
      prova={prova}
      chaveDaConsulta={chave}
      retiradas={retiradas}
    />
  );
}

function EsqueletoDaProva() {
  return (
    <div className="space-y-6" aria-hidden>
      <Skeleton className="h-6 w-2/3" />
      <Skeleton className="h-40 w-full" />
      <div className="space-y-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full rounded-lg" />
        ))}
      </div>
    </div>
  );
}
