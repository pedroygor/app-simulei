"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import type { Rotulo } from "@/lib/api/questoes";
import { criarSimulado } from "@/lib/api/simulados";
import { buscarTaxonomia } from "@/lib/api/taxonomia";
import { iniciarTentativa } from "@/lib/api/tentativas";
import { pedidoDeTreino, questoesNoBanco } from "@/lib/desempenho/treino";

const UMA_HORA = 60 * 60 * 1000;

/**
 * "Treinar": monta a prova do assunto, inicia e abre — um toque do diagnóstico ao
 * treino. A contagem vem da taxonomia, na MESMA chave de cache da listagem de
 * questões (quem já passou por lá não busca de novo).
 *
 * Montar NÃO é idempotente (cada POST cria um simulado), então o botão trava do
 * primeiro toque até a troca de página — dois toques não podem virar dois treinos.
 */
export function BotaoDeTreino({ assunto }: { assunto: Rotulo }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { data: taxonomia } = useQuery({
    queryKey: ["taxonomia"],
    queryFn: () => buscarTaxonomia(),
    staleTime: UMA_HORA,
  });

  const treino = useMutation({
    mutationFn: async () => {
      const pedido = pedidoDeTreino(assunto, taxonomia ? questoesNoBanco(taxonomia, assunto.slug) : 0);
      if (!pedido) throw new Error("Este assunto tem poucas questões no banco para um treino.");
      const simulado = await criarSimulado(pedido);
      return iniciarTentativa(simulado.id);
    },
    onSuccess: (tentativa) => {
      // A prova nova entra em "Minhas provas"
      void queryClient.invalidateQueries({ queryKey: ["simulados", "minhas"] });
      router.push(`/tentativas/${tentativa.id}`);
    },
  });

  if (taxonomia && !pedidoDeTreino(assunto, questoesNoBanco(taxonomia, assunto.slug))) {
    return <p className="text-muted-foreground text-xs">Poucas questões deste assunto no banco para montar um treino.</p>;
  }

  return (
    <div className="grid gap-1">
      <Button
        type="button"
        variant="outline"
        disabled={!taxonomia || treino.isPending || treino.isSuccess}
        onClick={() => treino.mutate()}
      >
        {treino.isPending || treino.isSuccess ? "Montando o treino…" : "Treinar este assunto"}
      </Button>
      {treino.error && (
        <p role="alert" className="text-destructive text-xs">
          {treino.error.message}
        </p>
      )}
    </div>
  );
}
