import { cva } from "class-variance-authority";
import { X } from "lucide-react";
import type { SituacaoNaRevisao } from "@/lib/prova/correcao";

// A grade de bolhas da identidade visual: PREENCHIDA = acertou. Errou ganha X
// (forma, não só cor), em branco é só contorno
const bolha = cva(
  "relative flex size-11 items-center justify-center rounded-full border-2 font-mono text-sm tabular-nums transition-colors focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none",
  {
    variants: {
      situacao: {
        acertou: "border-primary bg-marca-texto text-sobre-marca-texto font-semibold",
        errou: "border-destructive text-destructive",
        emBranco: "border-contorno text-muted-foreground border-dashed",
      },
    },
  },
);

const FALADO: Record<SituacaoNaRevisao, string> = {
  acertou: "acertou",
  errou: "errou",
  emBranco: "em branco",
};

// O X no canto da bolha: a forma que diz "errou" sem depender da cor
function MarcaDeErro() {
  return (
    <span aria-hidden className="bg-destructive text-background absolute -top-1 -right-1 flex size-4 items-center justify-center rounded-full">
      <X className="size-3" strokeWidth={3} />
    </span>
  );
}

/** Cada número abre a revisão da questão. Navegação, não seleção: botões. */
export function MapaDoResultado({
  questoes,
  aoAbrir,
}: {
  questoes: { ordem: number; situacao: SituacaoNaRevisao }[];
  aoAbrir: (indice: number) => void;
}) {
  return (
    <section aria-labelledby="questoes-da-prova" className="grid gap-3">
      <div>
        <h2 id="questoes-da-prova" className="font-heading text-lg font-bold">
          Questões
        </h2>
        <p className="text-muted-foreground text-sm">Toque numa questão pra ver o gabarito</p>
      </div>
      <ol className="grid grid-cols-[repeat(auto-fill,minmax(2.75rem,1fr))] gap-2">
        {questoes.map((q, indice) => (
          <li key={q.ordem} className="flex justify-center">
            <button type="button" onClick={() => aoAbrir(indice)} className={bolha({ situacao: q.situacao })}>
              {/* O número fica sempre: sem ele o aluno não saberia QUAL errou */}
              {q.ordem}
              {q.situacao === "errou" && <MarcaDeErro />}
              <span className="sr-only">, {FALADO[q.situacao]}</span>
            </button>
          </li>
        ))}
      </ol>
    </section>
  );
}
