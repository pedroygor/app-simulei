import Link from "next/link";
import { ChevronRight } from "lucide-react";
import type { ProvasAbertas } from "@/lib/api/tentativas";
import { progressoDaProva, quandoTermina } from "@/lib/painel/painel";

/**
 * O que está pela metade vem primeiro: prova com prazo vence sozinha. O cartão
 * inteiro é o link (alvo de toque grande); sem acertos — a prova está aberta.
 */
export function ContinuarProvas({ abertas }: { abertas: ProvasAbertas }) {
  return (
    <section aria-labelledby="continuar" className="grid gap-3">
      <h2 id="continuar" className="font-heading text-lg font-bold">
        Continuar
      </h2>
      <ul className="grid gap-3">
        {abertas.provas.map((prova) => (
          <li key={prova.tentativaId}>
            <Link
              href={`/tentativas/${prova.tentativaId}`}
              className="hover:bg-muted/50 flex items-center justify-between gap-3 rounded-lg border p-4"
            >
              <span className="grid min-w-0 gap-0.5">
                <span className="font-medium break-words">{prova.titulo}</span>
                <span className="text-muted-foreground text-sm tabular-nums">
                  {progressoDaProva(prova)}
                  {prova.prazo && ` · ${quandoTermina(prova.prazo, abertas.agora)}`}
                </span>
              </span>
              <ChevronRight aria-hidden className="text-muted-foreground size-5 shrink-0" />
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
