import { notFound } from "next/navigation";
import { RotaProtegida } from "@/components/auth/rota-protegida";
import { TelaDaProva } from "@/components/prova/tela-da-prova";
import { lerIdDaTentativa } from "@/lib/prova/prova";

export const metadata = {
  title: "Prova · Simulei",
};

// Sem cabeçalho de navegação de propósito: durante a prova, a tela é só a prova
export default async function PaginaDaProva({ params }: PageProps<"/tentativas/[id]">) {
  // `params` é promessa no Next 16. Id fora do formato UUID: 404 sem chamar a API
  const id = lerIdDaTentativa((await params).id);
  if (id === null) notFound();

  return (
    <RotaProtegida>
      <main className="mx-auto w-full max-w-2xl px-4 pt-6">
        <TelaDaProva key={id} id={id} />
      </main>
    </RotaProtegida>
  );
}
