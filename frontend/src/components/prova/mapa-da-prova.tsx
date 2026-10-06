import { cva } from "class-variance-authority";
import type { SituacaoDoSalvamento } from "@/lib/prova/fila-de-salvamento";

export type MarcadorDoMapa = {
  ordem: number;
  respondida: boolean;
  situacao: SituacaoDoSalvamento;
};

// A fileira de bolhas da identidade visual: respondida PREENCHE (papel sobre o
// fundo), em branco é só contorno, a atual ganha marca-texto. Preenchida × vazia
// é diferença de forma, não só de cor
const marcador = cva(
  "flex size-11 items-center justify-center rounded-full border-2 font-mono text-sm tabular-nums transition-colors focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none",
  {
    variants: {
      tipo: {
        emBranco: "border-contorno text-foreground",
        respondida: "border-foreground bg-foreground text-background",
        atual: "border-primary bg-marca-texto text-sobre-marca-texto font-semibold",
      },
      // Não salva: borda tracejada (forma) e vermelha (cor)
      falhou: { true: "border-destructive border-dashed", false: "" },
    },
  },
);

/**
 * Todas as questões da prova numa grade: ver o que falta e pular direto pra
 * qualquer uma. Uma lista de links de navegação, não um radiogroup: tocar num
 * número não responde nada.
 */
export function MapaDaProva({
  marcadores,
  atual,
  aoIr,
}: {
  marcadores: MarcadorDoMapa[];
  atual: number;
  aoIr: (indice: number) => void;
}) {
  return (
    <nav aria-label="Questões da prova">
      <ol className="grid grid-cols-[repeat(auto-fill,minmax(2.75rem,1fr))] gap-2">
        {marcadores.map((m, indice) => (
          <li key={m.ordem} className="flex justify-center">
            <button
              type="button"
              onClick={() => aoIr(indice)}
              aria-current={indice === atual ? "step" : undefined}
              className={marcador({
                tipo: indice === atual ? "atual" : m.respondida ? "respondida" : "emBranco",
                falhou: m.situacao === "erro",
              })}
            >
              {m.ordem}
              <span className="sr-only">
                {m.respondida ? ", respondida" : ", em branco"}
                {m.situacao === "erro" && ", resposta não salva"}
              </span>
            </button>
          </li>
        ))}
      </ol>
    </nav>
  );
}
