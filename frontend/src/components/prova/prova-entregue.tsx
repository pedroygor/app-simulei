import Link from "next/link";
import { CircleCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Prova } from "@/lib/api/tentativas";

const HORA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

/**
 * A prova já foi entregue. A correção (nota e gabarito) é a 3c; até lá, a tela
 * só confirma a entrega, sem prometer o que ainda não existe.
 */
export function ProvaEntregue({ prova }: { prova: Prova }) {
  const respondidas = prova.questoes.filter((q) => q.marcada !== null).length;

  return (
    <section aria-labelledby="prova-entregue" className="grid gap-4 py-6">
      <div className="flex items-center gap-2">
        <CircleCheck aria-hidden className="text-primary size-6" />
        <h1 id="prova-entregue" className="font-heading text-2xl font-bold">
          Prova entregue
        </h1>
      </div>
      <p className="text-sm">
        <span className="font-medium">{prova.titulo}</span> · {respondidas} de {prova.questoes.length} respondidas
      </p>
      {prova.finalizadoEm && (
        <p className="text-muted-foreground text-sm">Entregue em {HORA.format(new Date(prova.finalizadoEm))}</p>
      )}
      <Button asChild variant="outline" size="xl">
        <Link href="/inicio">Voltar ao início</Link>
      </Button>
    </section>
  );
}
