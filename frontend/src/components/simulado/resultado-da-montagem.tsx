"use client";

import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CircleCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { SimuladoCriado } from "@/lib/api/simulados";
import { iniciarTentativa } from "@/lib/api/tentativas";

/** O que o sorteio montou (quanto saiu de cada escolha) e o caminho pra fazer a prova. */
export function ResultadoDaMontagem({ simulado, aoMontarOutra }: { simulado: SimuladoCriado; aoMontarOutra: () => void }) {
  const router = useRouter();
  // Iniciar é idempotente no backend: dois toques no botão abrem a MESMA tentativa
  const inicio = useMutation({
    mutationFn: () => iniciarTentativa(simulado.id),
    onSuccess: (tentativa) => router.push(`/tentativas/${tentativa.id}`),
  });

  return (
    <section aria-labelledby="prova-montada" className="grid gap-4">
      <div className="flex items-center gap-2">
        <CircleCheck aria-hidden className="text-primary size-6" />
        <h2 id="prova-montada" className="font-heading text-xl font-bold">
          Prova montada
        </h2>
      </div>
      <p className="text-sm">
        <span className="font-medium">{simulado.titulo}</span> · {simulado.total} questões
        {simulado.tempoLimiteSeg !== null && ` · ${simulado.tempoLimiteSeg / 60} min`}
      </p>
      <ul className="grid gap-1 rounded-lg border p-3 text-sm" aria-label="Composição da prova">
        {simulado.composicao.map((parte) => (
          <li key={parte.rotulo} className="flex justify-between gap-4">
            <span className="min-w-0 break-words">{parte.rotulo}</span>
            <span className="tabular-nums">{parte.quantidade}</span>
          </li>
        ))}
      </ul>
      {inicio.error && (
        <p role="alert" className="text-destructive text-sm">
          {inicio.error.message}
        </p>
      )}
      {/* isSuccess também desabilita: entre o sucesso e a troca de página, um
          segundo toque não deve disparar outra navegação */}
      <Button type="button" size="xl" disabled={inicio.isPending || inicio.isSuccess} onClick={() => inicio.mutate()}>
        {inicio.isPending || inicio.isSuccess ? "Abrindo a prova…" : "Começar a prova"}
      </Button>
      <div className="grid gap-2 sm:grid-cols-2">
        <Button type="button" variant="outline" onClick={aoMontarOutra}>
          Montar outra
        </Button>
        <Button asChild variant="ghost">
          <Link href="/inicio">Voltar ao início</Link>
        </Button>
      </div>
    </section>
  );
}
