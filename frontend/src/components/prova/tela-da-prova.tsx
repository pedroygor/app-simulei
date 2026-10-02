"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
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
  return <ProvaEmAndamento prova={prova} chaveDaConsulta={chave} />;
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
