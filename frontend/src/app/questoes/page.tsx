import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { RotaProtegida } from "@/components/auth/rota-protegida";
import { ListaDeQuestoes } from "@/components/questao/lista-de-questoes";
import { lerFiltroDaUrl } from "@/lib/questoes/filtro-da-url";

export const metadata = {
  title: "Questões · Simulei",
};

/**
 * Server Component só pelo `await searchParams`.
 *
 * No Next 16 searchParams é uma PROMESSA — mudança que quebra código de
 * versões anteriores. Ler aqui e passar o filtro já validado adiante mantém a
 * URL como fonte da verdade: link de filtro é compartilhável e o botão voltar
 * funciona. A busca em si é client-side porque o token vive em memória.
 */
export default async function PaginaDeQuestoes({ searchParams }: PageProps<"/questoes">) {
  const filtro = lerFiltroDaUrl(await searchParams);

  return (
    <RotaProtegida>
      <main className="mx-auto w-full max-w-2xl px-4 py-6">
        <header className="mb-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Link
              href="/inicio"
              aria-label="Voltar para o início"
              className="text-muted-foreground hover:text-foreground"
            >
              <ChevronLeft aria-hidden className="size-5" />
            </Link>
            <h1 className="font-heading text-2xl font-bold tracking-tight">Questões</h1>
          </div>
          <AlternadorDeTema />
        </header>

        <ListaDeQuestoes filtro={filtro} />
      </main>
    </RotaProtegida>
  );
}
