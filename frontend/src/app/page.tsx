import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { Button } from "@/components/ui/button";

const AREAS = [
  { nome: "Linguagens", classe: "bg-area-linguagens" },
  { nome: "Humanas", classe: "bg-area-humanas" },
  { nome: "Natureza", classe: "bg-area-natureza" },
  { nome: "Matemática", classe: "bg-area-matematica" },
] as const;

/**
 * Página de verificação da identidade visual. Existe pra conferir os tokens
 * nos dois temas antes de construir tela de verdade — vai ser substituída
 * pela home do produto na próxima fatia.
 */
export default function Home() {
  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <header className="mb-10 flex items-start justify-between gap-4">
        <div>
          <h1 className="font-heading text-4xl font-bold tracking-tight">Simulei</h1>
          <p className="mt-1 text-muted-foreground">Verificação da identidade visual</p>
        </div>
        <AlternadorDeTema />
      </header>

      <section className="mb-10">
        <h2 className="font-heading mb-3 text-lg font-semibold">Ação primária</h2>
        <div className="flex flex-wrap gap-3">
          <Button>Começar simulado</Button>
          <Button variant="outline">Ver questões</Button>
          <Button variant="ghost">Cancelar</Button>
        </div>
      </section>

      <section className="mb-10">
        <h2 className="font-heading mb-3 text-lg font-semibold">Marca-texto</h2>
        <p className="text-muted-foreground">
          Usado com a disciplina de um marca-texto de verdade:{" "}
          <mark className="bg-marca-texto text-sobre-marca-texto rounded px-1.5 py-0.5 font-medium">
            só no que importa
          </mark>
          .
        </p>
      </section>

      <section className="mb-10">
        <h2 className="font-heading mb-3 text-lg font-semibold">Áreas do ENEM</h2>
        <ul className="grid grid-cols-2 gap-2">
          {AREAS.map((area) => (
            <li key={area.nome} className="bg-card flex items-center gap-2 rounded-lg border p-3">
              {/* A cor codifica a área, mas nunca vem sozinha: o rótulo ao lado
                  é o que faz a informação chegar a quem não distingue as cores */}
              <span className={`size-3 shrink-0 rounded-full ${area.classe}`} aria-hidden />
              <span className="text-sm">{area.nome}</span>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h2 className="font-heading mb-3 text-lg font-semibold">Dados</h2>
        <p className="font-mono text-muted-foreground text-sm">
          ENEM 2023 · 45 min 00 s · 182 questões
        </p>
      </section>
    </main>
  );
}
