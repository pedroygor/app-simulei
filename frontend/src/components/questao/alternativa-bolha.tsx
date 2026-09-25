import { cva } from "class-variance-authority";
import { Check } from "lucide-react";
import type { Alternativa } from "@/lib/api/questoes";
import { ImagemDaQuestao } from "./imagem-da-questao";
import { TextoDaQuestao } from "./texto-da-questao";

/**
 * A bolha do cartão-resposta — o componente-assinatura do Simulei (ver
 * docs/identidade-visual.md). Aqui só nos estados de LEITURA: `neutra` e
 * `correta` (gabarito revelado).
 *
 * `selecionada`, `incorreta` e `desabilitada` entram na Etapa 3, quando existir
 * Tentativa pra guardar a resposta. Junto com eles vem a semântica de radio
 * group — hoje ela seria mentira: anunciar "opção 1 de 5, não selecionada"
 * pra algo que não aceita seleção confunde quem usa leitor de tela.
 */
export type EstadoDaBolha = "neutra" | "correta";

const cartao = cva("flex items-start gap-3 rounded-lg border p-3 transition-colors", {
  variants: {
    estado: {
      neutra: "border-border bg-card",
      // --primary e não --marca-texto: o marca-texto puro dá 1.25 de
      // contraste sobre fundo claro. O primary é a mesma matiz limão ajustada
      // por tema (#5F6E0F no claro, #D4F03C no escuro)
      correta: "border-primary bg-card",
    },
  },
});

// Contorno com o token --contorno, não --border: a bolha é contorno de
// controle e precisa de 3:1 (WCAG 1.4.11). O hairline decorativo dá 1.43
const bolha = cva(
  "flex size-8 shrink-0 items-center justify-center rounded-full border-2 font-mono text-sm font-semibold",
  {
    variants: {
      estado: {
        neutra: "border-contorno text-foreground",
        // Marca-texto no papel natural dele: FUNDO de destaque com o grafite
        // próprio por cima (11.91 nos dois temas), nunca preto puro. A borda
        // em primary dá contorno à bolha no tema claro, onde amarelo sobre
        // branco não tem borda visível
        correta: "border-primary bg-marca-texto text-sobre-marca-texto",
      },
    },
  },
);

export function AlternativaBolha({
  alternativa,
  estado,
}: {
  alternativa: Alternativa;
  estado: EstadoDaBolha;
}) {
  const correta = estado === "correta";

  return (
    <div className={cartao({ estado })}>
      <span className={bolha({ estado })} aria-hidden>
        {/* Forma muda junto com a cor: letra vira check. Cor sozinha não
            serve de sinal pra quem não distingue as cores */}
        {correta ? <Check className="size-4" strokeWidth={3} /> : alternativa.letra}
      </span>

      <div className="min-w-0 flex-1 pt-1">
        {/* A letra fica visível pro leitor de tela mesmo quando a bolha mostra
            o check, e "gabarito" é dito em texto, não só em cor */}
        <span className="sr-only">
          Alternativa {alternativa.letra}
          {correta && ", gabarito"}:{" "}
        </span>
        <ConteudoDaAlternativa alternativa={alternativa} />
        {correta && (
          // text-primary: como TEXTO, o marca-texto puro some no tema claro
          <p className="text-primary mt-2 text-xs font-semibold tracking-wide uppercase" aria-hidden>
            Gabarito
          </p>
        )}
      </div>
    </div>
  );
}

/** 715 alternativas da fonte são SÓ imagem; nenhuma tem texto e imagem juntos. */
function ConteudoDaAlternativa({ alternativa }: { alternativa: Alternativa }) {
  if (alternativa.imagemUrl) {
    return <ImagemDaQuestao src={alternativa.imagemUrl} alt={`Alternativa ${alternativa.letra}`} />;
  }
  return <TextoDaQuestao texto={alternativa.texto} className="text-sm" />;
}
