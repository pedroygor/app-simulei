import Link from "next/link";
import type { Rotulo } from "@/lib/api/questoes";
import { rotaDoAssunto } from "@/lib/desempenho/rotas";

/** O nome do assunto como link pra evolução dele. Sublinhado: link não pode ser só cor. */
export function LinkDoAssunto({ assunto }: { assunto: Rotulo }) {
  return (
    <Link href={rotaDoAssunto(assunto.slug)} className="underline decoration-1 underline-offset-4 hover:decoration-2">
      {assunto.nome}
    </Link>
  );
}
