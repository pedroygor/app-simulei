"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { buscarMinhasProvas } from "@/lib/api/simulados";
import { proximaPagina } from "@/lib/simulados/historico";
import { CartaoDaProva } from "./cartao-da-prova";

/**
 * "Minhas provas", da mais nova pra mais velha, 20 por vez com "carregar mais".
 * useInfiniteQuery: cada página é uma consulta em cache, e "carregar mais" pede
 * só a próxima — as anteriores não são buscadas de novo.
 *
 * Sem staleTime: a lista muda toda vez que o aluno monta, começa ou entrega uma
 * prova, então voltar pra cá deve refletir isso.
 */
export function MinhasProvas() {
  const consulta = useInfiniteQuery({
    queryKey: ["simulados", "minhas"],
    queryFn: ({ pageParam }) => buscarMinhasProvas(pageParam),
    initialPageParam: 0,
    getNextPageParam: proximaPagina,
  });

  if (consulta.isPending) return <EsqueletoDaLista />;
  if (consulta.error) {
    return (
      <p role="alert" className="text-destructive py-8 text-center text-sm">
        {consulta.error.message}
      </p>
    );
  }

  const provas = consulta.data.pages.flatMap((pagina) => pagina.itens);
  if (provas.length === 0) return <SemProvas />;
  const total = consulta.data.pages[0].totalDeItens;

  return (
    <div className="grid gap-4 pb-8">
      <p className="text-muted-foreground text-sm tabular-nums">
        {total === 1 ? "1 prova" : `${total} provas`}
      </p>
      <ul className="grid gap-3">
        {provas.map((prova) => (
          <CartaoDaProva key={prova.simuladoId} prova={prova} />
        ))}
      </ul>
      {consulta.hasNextPage && (
        <Button
          type="button"
          variant="outline"
          onClick={() => consulta.fetchNextPage()}
          disabled={consulta.isFetchingNextPage}
        >
          {consulta.isFetchingNextPage ? "Carregando…" : "Carregar mais"}
        </Button>
      )}
    </div>
  );
}

function SemProvas() {
  return (
    <div className="grid gap-3 py-8 text-center">
      <p className="text-muted-foreground text-sm">Você ainda não montou nenhuma prova.</p>
      <Button asChild size="xl">
        <Link href="/simulados/novo">Montar prova</Link>
      </Button>
    </div>
  );
}

function EsqueletoDaLista() {
  return (
    <div className="grid gap-3" aria-hidden>
      {Array.from({ length: 4 }).map((_, i) => (
        <Skeleton key={i} className="h-20 w-full rounded-lg" />
      ))}
    </div>
  );
}
