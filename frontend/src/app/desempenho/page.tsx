import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { RotaProtegida } from "@/components/auth/rota-protegida";
import { PainelDeDesempenho } from "@/components/desempenho/painel-de-desempenho";

export const metadata = {
  title: "Meu desempenho · Simulei",
};

export default function PaginaDeDesempenho() {
  return (
    <RotaProtegida>
      <main className="mx-auto w-full max-w-2xl px-4 pt-6">
        <header className="mb-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Link href="/inicio" aria-label="Voltar para o início" className="text-muted-foreground hover:text-foreground">
              <ChevronLeft aria-hidden className="size-5" />
            </Link>
            <h1 className="font-heading text-2xl font-bold tracking-tight">Meu desempenho</h1>
          </div>
          <AlternadorDeTema />
        </header>

        <PainelDeDesempenho />
      </main>
    </RotaProtegida>
  );
}
