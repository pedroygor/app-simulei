import Link from "next/link";
import { Button } from "@/components/ui/button";

const PASSOS = [
  "Monte uma prova escolhendo áreas, matérias ou assuntos, com questões de ENEMs anteriores.",
  "Responda pelo celular, no seu tempo ou com cronômetro.",
  "Veja onde você erra, por área e por assunto, e treine o que precisa.",
];

/** Aluno sem prova nenhuma: em vez de cartões vazios, o caminho em três passos. */
export function ComecoGuiado() {
  return (
    <section aria-labelledby="comece" className="grid gap-4 rounded-lg border p-4">
      <h2 id="comece" className="font-heading text-xl font-bold">
        Comece pela sua primeira prova
      </h2>
      <ol className="grid list-decimal gap-2 pl-5 text-sm">
        {PASSOS.map((passo) => (
          <li key={passo}>{passo}</li>
        ))}
      </ol>
      <div className="grid gap-2">
        <Button asChild size="xl" className="w-full">
          <Link href="/simulados/novo">Montar minha primeira prova</Link>
        </Button>
        <Button asChild variant="outline" className="w-full">
          <Link href="/questoes">Explorar questões antes</Link>
        </Button>
      </div>
    </section>
  );
}
