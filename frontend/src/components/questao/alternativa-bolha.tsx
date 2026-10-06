import { cva } from "class-variance-authority";
import { Check, X } from "lucide-react";
import type { KeyboardEvent, Ref } from "react";
import type { Alternativa } from "@/lib/api/questoes";
import { escaparMarcadoresDeBloco } from "@/lib/questoes/markdown";
import { ImagemDaQuestao } from "./imagem-da-questao";
import { TextoDaQuestao } from "./texto-da-questao";

/**
 * A bolha do cartão-resposta — o componente-assinatura do Simulei (ver
 * docs/identidade-visual.md).
 *
 * Dois modos:
 *   - LEITURA (sem `radio`): `neutra` ou `correta` no detalhe da questão; na
 *     revisão do resultado (3c), também `acertou` e `incorreta`
 *   - ESCOLHA (com `radio`): `neutra` ou `selecionada`, na prova (3b). Vira
 *     role="radio" — quem monta o radiogroup e o teclado é o GrupoDeAlternativas
 *
 * Nenhum estado é só cor (identidade visual): o gabarito vira CHECK, o erro vira
 * X, a marcada tem contorno duplo, e o estado é dito em texto.
 */
export type EstadoDaBolha =
  | "neutra"
  | "selecionada"
  /** Gabarito que o aluno NÃO marcou (ou o detalhe da questão, sem prova). */
  | "correta"
  /** O aluno marcou o gabarito. */
  | "acertou"
  /** O aluno marcou e não é o gabarito. */
  | "incorreta";

/** O que o radiogroup passa pra cada opção. */
export type PropsDeRadio = {
  /** Roving tabindex: só uma opção do grupo fica na ordem do Tab. */
  focavel: boolean;
  aoEscolher: () => void;
  aoTeclar: (evento: KeyboardEvent<HTMLDivElement>) => void;
};

const cartao = cva("flex items-start gap-3 rounded-lg border p-3 transition-colors", {
  variants: {
    estado: {
      neutra: "border-border bg-card",
      // Contorno DUPLO (borda + ring) e não só a cor da borda: a forma muda
      // junto, e o ring não desloca o layout como border-2 deslocaria
      selecionada: "border-primary ring-primary bg-card ring-1",
      // --primary e não --marca-texto: o marca-texto puro dá 1.25 de
      // contraste sobre fundo claro. O primary é a mesma matiz limão ajustada
      // por tema (#5F6E0F no claro, #D4F03C no escuro)
      correta: "border-primary bg-card",
      // Marcada (contorno duplo, como a selecionada) E gabarito
      acertou: "border-primary ring-primary bg-card ring-1",
      // Marcada e errada: contorno duplo no tom de erro
      incorreta: "border-destructive ring-destructive bg-card ring-1",
    },
    interativa: {
      true: "focus-visible:ring-ring cursor-pointer select-none focus-visible:ring-2 focus-visible:outline-none",
      false: "",
    },
  },
  compoundVariants: [{ estado: "neutra", interativa: true, className: "hover:border-contorno" }],
});

// Contorno com o token --contorno, não --border: a bolha é contorno de
// controle e precisa de 3:1 (WCAG 1.4.11). O hairline decorativo dá 1.43
const bolha = cva(
  "flex size-8 shrink-0 items-center justify-center rounded-full border-2 font-mono text-sm font-semibold",
  {
    variants: {
      estado: {
        neutra: "border-contorno text-foreground",
        // A bolha PREENCHE, como no cartão-resposta a lápis. Marca-texto no
        // papel natural dele: fundo de destaque com o grafite próprio por cima
        // (11.91 nos dois temas). A borda em primary dá contorno no tema claro
        selecionada: "border-primary bg-marca-texto text-sobre-marca-texto",
        correta: "border-primary bg-marca-texto text-sobre-marca-texto",
        acertou: "border-primary bg-marca-texto text-sobre-marca-texto",
        // NÃO preenche de vermelho: texto claro sobre o --destructive do tema
        // escuro não tem contraste. O que muda é a FORMA (o X) e o texto
        incorreta: "border-destructive text-destructive",
      },
    },
  },
);

// text-primary e não marca-texto: como TEXTO, o marca-texto puro some no claro
const rotuloDoEstado = cva("mt-2 text-xs font-semibold tracking-wide uppercase", {
  variants: {
    tom: { acerto: "text-primary", erro: "text-destructive" },
  },
});

const ROTULO_DO_ESTADO: Record<EstadoDaBolha, string | null> = {
  neutra: null,
  selecionada: "Sua resposta",
  correta: "Gabarito",
  acertou: "Sua resposta · Gabarito",
  incorreta: "Sua resposta",
};

// O que o leitor de tela ouve depois da letra (a bolha e o rótulo são aria-hidden)
const ESTADO_FALADO: Record<EstadoDaBolha, string> = {
  neutra: "",
  // Na prova o "marcado" já vem do aria-checked do radio
  selecionada: "",
  correta: ", gabarito",
  acertou: ", sua resposta, correta",
  incorreta: ", sua resposta, errada",
};

function SimboloDaBolha({ estado, letra }: { estado: EstadoDaBolha; letra: string }) {
  if (estado === "correta" || estado === "acertou") return <Check className="size-4" strokeWidth={3} />;
  if (estado === "incorreta") return <X className="size-4" strokeWidth={3} />;
  // A selecionada mantém a letra: não pode se confundir com o gabarito
  return <>{letra}</>;
}

export function AlternativaBolha({
  alternativa,
  estado,
  radio,
  ref,
}: {
  alternativa: Alternativa;
  estado: EstadoDaBolha;
  radio?: PropsDeRadio;
  // React 19: ref é prop comum. Fora do objeto `radio` de propósito — o lint do
  // React Compiler trataria o objeto inteiro como ref e recusaria lê-lo no render
  ref?: Ref<HTMLDivElement>;
}) {
  const rotulo = ROTULO_DO_ESTADO[estado];
  const conteudo = (
    <>
      <span className={bolha({ estado })} aria-hidden>
        <SimboloDaBolha estado={estado} letra={alternativa.letra} />
      </span>

      <div className="min-w-0 flex-1 pt-1">
        {/* A letra vai em texto pro leitor de tela (a da bolha é aria-hidden).
            No modo escolha ela vira o começo do NOME do radio, calculado do
            conteúdo: "Alternativa B: <texto>". O "marcado" vem do aria-checked */}
        <span className="sr-only">
          Alternativa {alternativa.letra}
          {ESTADO_FALADO[estado]}:{" "}
        </span>
        <ConteudoDaAlternativa alternativa={alternativa} />
        {rotulo && (
          // Estado dito em TEXTO, não só em cor
          <p className={rotuloDoEstado({ tom: estado === "incorreta" ? "erro" : "acerto" })} aria-hidden>
            {rotulo}
          </p>
        )}
      </div>
    </>
  );

  if (!radio) {
    return <div className={cartao({ estado, interativa: false })}>{conteudo}</div>;
  }

  return (
    // div e não <button>: o conteúdo é markdown renderizado (parágrafos,
    // imagem), e <button> só aceita conteúdo inline — <p> dentro dele é HTML
    // inválido. O padrão de radio do WAI-ARIA é exatamente uma div com
    // role="radio" e teclado próprio, que é o que o grupo fornece
    <div
      ref={ref}
      role="radio"
      aria-checked={estado === "selecionada"}
      tabIndex={radio.focavel ? 0 : -1}
      onClick={radio.aoEscolher}
      onKeyDown={radio.aoTeclar}
      className={cartao({ estado, interativa: true })}
    >
      {conteudo}
    </div>
  );
}

/** 715 alternativas da fonte são SÓ imagem; nenhuma tem texto e imagem juntos. */
function ConteudoDaAlternativa({ alternativa }: { alternativa: Alternativa }) {
  if (alternativa.imagemUrl) {
    return <ImagemDaQuestao src={alternativa.imagemUrl} alt={`Alternativa ${alternativa.letra}`} />;
  }
  // Alternativa é resposta, nunca lista: "9." tem que aparecer como "9." (ver
  // escaparMarcadoresDeBloco — 325 alternativas apareciam em branco)
  return (
    <TextoDaQuestao
      texto={alternativa.texto && escaparMarcadoresDeBloco(alternativa.texto)}
      className="text-sm"
    />
  );
}
