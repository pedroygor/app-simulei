import Link from "next/link";
import { notFound } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { RotaProtegida } from "@/components/auth/rota-protegida";
import { DesempenhoNoAssunto } from "@/components/desempenho/desempenho-no-assunto";
import { lerSlug } from "@/lib/questoes/filtro-da-url";

export const metadata = {
  title: "Desempenho no assunto · Simulei",
};

/** `params` é promessa no Next 16. Slug fora do formato nem chega na API: 404 aqui mesmo. */
export default async function PaginaDoAssunto({ params }: PageProps<"/desempenho/assuntos/[slug]">) {
  const slug = lerSlug((await params).slug);
  if (!slug) notFound();

  return (
    <RotaProtegida>
      <main className="mx-auto w-full max-w-2xl px-4 py-6">
        <nav className="mb-6 flex items-center justify-between gap-4">
          <Link
            href="/desempenho"
            className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
          >
            <ChevronLeft aria-hidden className="size-4" />
            Meu desempenho
          </Link>
          <AlternadorDeTema />
        </nav>
        {/* key: trocar de assunto não pode herdar a coluna escolhida no gráfico anterior */}
        <DesempenhoNoAssunto key={slug} slug={slug} />
      </main>
    </RotaProtegida>
  );
}
