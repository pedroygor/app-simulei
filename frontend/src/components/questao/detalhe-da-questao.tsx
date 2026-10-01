"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ErroDaApi } from "@/lib/api/erros";
import { buscarQuestaoPorId, type QuestaoDetalhe } from "@/lib/api/questoes";
import { NOME_DO_IDIOMA } from "@/lib/questoes/filtro-da-url";
import { AlternativaBolha } from "./alternativa-bolha";
import { EtiquetaDeArea } from "./etiqueta-de-area";
import { EtiquetaDeAssunto } from "./etiqueta-de-assunto";
import { HabilidadeDaMatriz } from "./habilidade-da-matriz";
import { TextoDaQuestao } from "./texto-da-questao";

/**
 * Client Component pelo mesmo motivo da listagem: o token vive em memória e um
 * Server Component não teria como autenticar a chamada.
 */
export function DetalheDaQuestao({ id, voltarPara }: { id: number; voltarPara: string }) {
  const { data: questao, isPending, error } = useQuery({
    queryKey: ["questoes", "detalhe", id],
    queryFn: () => buscarQuestaoPorId(id),
    // Questão importada não muda — a mesma prova do ENEM é a mesma pra sempre
    staleTime: Infinity,
  });

  if (isPending) return <EsqueletoDoDetalhe />;

  if (error) {
    // 404 cobre inexistente, anulada e incompleta (V9): pro produto, elas não existem
    const naoExiste = error instanceof ErroDaApi && error.status === 404;
    return (
      <div className="space-y-3 py-8 text-center text-sm">
        <p role="alert" className={naoExiste ? "text-muted-foreground" : "text-destructive"}>
          {naoExiste ? "Esta questão não existe ou não está disponível." : error.message}
        </p>
        <Link href={voltarPara} className="underline">
          Voltar para as questões
        </Link>
      </div>
    );
  }

  return <ConteudoDaQuestao questao={questao} />;
}

function ConteudoDaQuestao({ questao }: { questao: QuestaoDetalhe }) {
  const [gabaritoVisivel, setGabaritoVisivel] = useState(false);

  return (
    <article className="space-y-6">
      <header className="space-y-2">
        <h1 className="font-heading text-2xl font-bold tracking-tight">
          ENEM {questao.ano} · questão {questao.numero}
        </h1>
        <div className="text-muted-foreground flex flex-wrap items-center gap-x-3 gap-y-1">
          <EtiquetaDeArea area={questao.area} />
          {questao.idioma && <Badge variant="outline">{NOME_DO_IDIOMA[questao.idioma]}</Badge>}
          <EtiquetaDeAssunto materia={questao.materia} assunto={questao.assunto} comMateria />
        </div>
      </header>

      {/* 5% das questões não têm enunciado (migration V3): o componente não
          renderiza nada pra texto nulo, sem deixar caixa vazia */}
      <TextoDaQuestao texto={questao.enunciado} />
      <TextoDaQuestao texto={questao.comando} className="font-medium" />

      <ol aria-label="Alternativas" className="space-y-3">
        {questao.alternativas.map((alternativa) => (
          <li key={alternativa.id}>
            <AlternativaBolha
              alternativa={alternativa}
              estado={gabaritoVisivel && alternativa.letra === questao.gabarito ? "correta" : "neutra"}
            />
          </li>
        ))}
      </ol>

      {questao.gabarito && (
        <div className="space-y-2">
          <Button
            className="w-full sm:w-auto"
            variant={gabaritoVisivel ? "outline" : "default"}
            // aria-pressed: é um botão de alternância, e o leitor de tela
            // precisa saber em que estado ele está
            aria-pressed={gabaritoVisivel}
            onClick={() => setGabaritoVisivel((visivel) => !visivel)}
          >
            {gabaritoVisivel ? "Esconder gabarito" : "Ver gabarito"}
          </Button>
          {/* Revelar só muda cor e ícone lá em cima, longe do botão: sem este
              anúncio, quem usa leitor de tela não saberia a resposta */}
          <p role="status" aria-live="polite" className="sr-only">
            {gabaritoVisivel ? `Gabarito: alternativa ${questao.gabarito}` : ""}
          </p>
        </div>
      )}

      {/* Depois das alternativas e do gabarito, não no cabeçalho: lida antes de
          responder, a habilidade ("calcular probabilidade") daria dica */}
      <HabilidadeDaMatriz habilidade={questao.habilidade} />
    </article>
  );
}

function EsqueletoDoDetalhe() {
  return (
    <div className="space-y-6" aria-hidden>
      <Skeleton className="h-8 w-3/4" />
      <Skeleton className="h-40 w-full" />
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full rounded-lg" />
        ))}
      </div>
    </div>
  );
}
