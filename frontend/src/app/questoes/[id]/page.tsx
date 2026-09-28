import Link from "next/link";
import { notFound } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { RotaProtegida } from "@/components/auth/rota-protegida";
import { DetalheDaQuestao } from "@/components/questao/detalhe-da-questao";
import { destinoDeVolta, lerIdDaQuestao } from "@/lib/questoes/navegacao";

export const metadata = {
  title: "Questão · Simulei",
};

/**
 * No Next 16 `params` é PROMESSA, igual ao searchParams — mesma mudança que
 * quebra código de versões anteriores.
 */
export default async function PaginaDaQuestao({ params, searchParams }: PageProps<"/questoes/[id]">) {
  const id = lerIdDaQuestao((await params).id);
  // Id malformado nem chega na API: /questoes/abc é 404 aqui mesmo
  if (id === null) notFound();

  const voltarPara = destinoDeVolta((await searchParams).voltar);

  return (
    <RotaProtegida>
      <main className="mx-auto w-full max-w-2xl px-4 py-6">
        <nav className="mb-6 flex items-center justify-between gap-4">
          <Link
            href={voltarPara}
            className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
          >
            <ChevronLeft aria-hidden className="size-4" />
            Questões
          </Link>
          <AlternadorDeTema />
        </nav>

        {/* key={id}: navegar de uma questão pra outra reaproveita a MESMA
            instância do componente (só as props mudam), e o gabarito revelado
            na anterior continuaria aberto na próxima */}
        <DetalheDaQuestao key={id} id={id} voltarPara={voltarPara} />
      </main>
    </RotaProtegida>
  );
}
