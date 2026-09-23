"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Skeleton } from "@/components/ui/skeleton";
import { buscarQuestoes, QUESTOES_POR_PAGINA, type FiltroDeQuestoes } from "@/lib/api/questoes";
import { montarUrlDeQuestoes } from "@/lib/questoes/filtro-da-url";
import { FiltrosDeQuestoes } from "./filtros-de-questoes";
import { Paginacao } from "./paginacao";
import { QuestaoCard } from "./questao-card";

/**
 * Busca e lista as questões.
 *
 * É Client Component por necessidade, não por preferência: o access token vive
 * em MEMÓRIA (decisão da fatia 5b), então um Server Component não teria como
 * autenticar a chamada. Quem lê a URL é a página, que é Server Component — o
 * filtro chega aqui já validado, por props.
 */
export function ListaDeQuestoes({ filtro }: { filtro: FiltroDeQuestoes }) {
  const { data, isPending, isError, error } = useQuery({
    // O filtro inteiro entra na chave: trocar de área ou de página é outra
    // consulta, com cache próprio — voltar pra página anterior é instantâneo
    queryKey: ["questoes", filtro],
    queryFn: () => buscarQuestoes(filtro),
  });

  return (
    <div className="space-y-6">
      <FiltrosDeQuestoes filtro={filtro} />

      {isPending && <EsqueletoDaLista />}

      {isError && (
        <p role="alert" className="text-destructive text-sm">
          {error instanceof Error ? error.message : "Não foi possível carregar as questões."}
        </p>
      )}

      {data && data.itens.length === 0 && (
        <div className="text-muted-foreground py-8 text-center text-sm">
          {data.pagina > 0 && data.totalDePaginas > 0 ? (
            /*
              Página além do fim — só se chega aqui editando a URL na mão
              (?pagina=100). Sem este caso, a paginação anunciaria "100 de 3"
              e o usuário ficaria numa tela vazia sem saída óbvia.
            */
            <>
              <p>Esta página não existe para os filtros atuais.</p>
              <Link href={montarUrlDeQuestoes({ ...filtro, pagina: 0 })} className="underline">
                Voltar para a primeira página
              </Link>
            </>
          ) : (
            <p>Nenhuma questão encontrada com esses filtros.</p>
          )}
        </div>
      )}

      {data && data.itens.length > 0 && (
        <>
          <p className="text-muted-foreground text-sm tabular-nums">
            {data.totalDeItens} {data.totalDeItens === 1 ? "questão" : "questões"}
          </p>
          <ul className="space-y-3">
            {data.itens.map((questao) => (
              <li key={questao.id}>
                <QuestaoCard questao={questao} />
              </li>
            ))}
          </ul>
          <Paginacao
            filtro={filtro}
            paginaAtual={data.pagina}
            totalDePaginas={data.totalDePaginas}
            totalDeQuestoes={data.totalDeItens}
          />
        </>
      )}
    </div>
  );
}

function EsqueletoDaLista() {
  return (
    // Mesma altura de um cartão real, pra lista não pular quando os dados
    // chegam — salto de layout no celular faz o dedo acertar o item errado
    <ul className="space-y-3" aria-hidden>
      {Array.from({ length: QUESTOES_POR_PAGINA }).map((_, i) => (
        <li key={i}>
          <Skeleton className="h-[104px] w-full rounded-xl" />
        </li>
      ))}
    </ul>
  );
}
