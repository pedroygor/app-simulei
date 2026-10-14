"use client";

import { useMutation } from "@tanstack/react-query";
import { ChevronRight } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import type { ProvaDoHistorico } from "@/lib/api/simulados";
import { iniciarTentativa } from "@/lib/api/tentativas";
import { destinoDaProva, resumoDaProva } from "@/lib/simulados/historico";
import { SeloDaSituacao } from "./selo-da-situacao";

const DATA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });

/**
 * Uma prova da lista. Iniciada: o cartão INTEIRO é o link (alvo de toque
 * grande). Não iniciada: não há tentativa pra abrir — o cartão tem um botão
 * "Começar", que cria a tentativa (POST idempotente) e navega.
 */
export function CartaoDaProva({ prova }: { prova: ProvaDoHistorico }) {
  const destino = destinoDaProva(prova);
  const conteudo = (
    <>
      <div className="flex items-start justify-between gap-3">
        <h2 className="min-w-0 font-medium break-words">{prova.titulo}</h2>
        <SeloDaSituacao situacao={prova.situacao} />
      </div>
      <p className="text-muted-foreground text-sm tabular-nums">
        {resumoDaProva(prova)} · {DATA.format(new Date(prova.finalizadoEm ?? prova.criadoEm))}
      </p>
    </>
  );

  if (destino) {
    return (
      <MolduraDoCartao>
        <Link href={destino} className="focus-visible:ring-ring flex items-center gap-3 rounded-lg p-4 focus-visible:ring-2 focus-visible:outline-none">
          <div className="grid min-w-0 flex-1 gap-1">{conteudo}</div>
          <ChevronRight aria-hidden className="text-muted-foreground size-5 shrink-0" />
        </Link>
      </MolduraDoCartao>
    );
  }

  return (
    <MolduraDoCartao>
      <div className="grid gap-3 p-4">
        <div className="grid gap-1">{conteudo}</div>
        <BotaoComecar simuladoId={prova.simuladoId} />
      </div>
    </MolduraDoCartao>
  );
}

function MolduraDoCartao({ children }: { children: ReactNode }) {
  return <li className="bg-card hover:border-contorno rounded-lg border transition-colors">{children}</li>;
}

function BotaoComecar({ simuladoId }: { simuladoId: string }) {
  const router = useRouter();
  const inicio = useMutation({
    mutationFn: () => iniciarTentativa(simuladoId),
    onSuccess: (tentativa) => router.push(`/tentativas/${tentativa.id}`),
  });
  // isSuccess também trava: entre o sucesso e a troca de página, nada de 2º toque
  const ocupado = inicio.isPending || inicio.isSuccess;

  return (
    <div className="grid gap-1">
      {/* outline: numa lista com várias provas não iniciadas, o botão cheio
          viraria uma parede de marca-texto (identidade visual: um destaque por tela) */}
      <Button type="button" variant="outline" onClick={() => inicio.mutate()} disabled={ocupado}>
        {ocupado ? "Abrindo a prova…" : "Começar a prova"}
      </Button>
      {inicio.error && (
        <p role="alert" className="text-destructive text-sm">
          {inicio.error.message}
        </p>
      )}
    </div>
  );
}
