import { cva } from "class-variance-authority";
import { cn } from "@/lib/utils";

/**
 * Turma de mentira, escrita À MÃO — não gerada por fórmula nem Math.random().
 * Random no render daria um desenho no servidor e outro no cliente (erro de
 * hidratação); e escrita à mão a história fica controlada: a questão 9 é a
 * que 5 de 6 alunos erraram, que é exatamente o que o professor precisa ver.
 * "1" = acertou, "0" = errou.
 */
const TURMA = [
  { aluno: "Ana", respostas: "110111110111" },
  { aluno: "Bruno", respostas: "101110110111" },
  { aluno: "Carla", respostas: "111101110101" },
  { aluno: "Davi", respostas: "011011100111" },
  { aluno: "Elisa", respostas: "111111011110" },
  { aluno: "Felipe", respostas: "110111110011" },
];
const QUESTOES = 12;
const CRITICA = 8; // índice: é a questão 9
const ERRARAM_A_CRITICA = TURMA.filter((a) => a.respostas[CRITICA] === "0").length;

// Mesma linguagem da GradeCartaoResposta (docs/identidade-visual.md):
// preenchida = acertou. "errou" usa --contorno (3:1), não o hairline — no
// design original a bolha vazia dava 1.45 e sumia
const bolha = cva("block shrink-0 rounded-full", {
  variants: {
    resultado: {
      acertou: "bg-foreground",
      errou: "border-contorno border-2",
    },
    tamanho: {
      // Fluida: 70% da largura da coluna, quadrada. Cresce junto com o
      // cartão — com tamanho fixo (20px) o quadro parecia miniatura num
      // painel de 700px
      grade: "aspect-square w-[70%]",
      legenda: "size-3",
    },
    // Coluna da questão crítica: a cor vai NA BOLHA, não numa faixa atrás
    // dela — com a faixa, as bolhas vazadas pareciam sem cor e o verde
    // "vazava" em volta. A forma continua dizendo o resultado (cheia =
    // acertou, vazada = errou); a cor só diz "é esta coluna".
    // --primary e não --marca-texto: anel amarelo sobre fundo claro dá 1.25
    destaque: { true: "", false: "" },
  },
  compoundVariants: [
    { destaque: true, resultado: "acertou", className: "bg-primary" },
    { destaque: true, resultado: "errou", className: "border-primary bg-primary/15" },
  ],
  defaultVariants: { tamanho: "grade", destaque: false },
});

/**
 * Prévia do painel do professor (a GradeCartaoResposta de verdade vem na
 * Etapa 4). Dado ILUSTRATIVO, por isso aria-hidden: anunciar 72 bolhas
 * inventadas seria ruído pro leitor de tela; o sentido está na frase ao lado.
 */
export function GradeIlustrativa() {
  return (
    <div aria-hidden className="bg-background w-full max-w-2xl rounded-2xl border p-5 shadow-sm xl:p-7 2xl:max-w-3xl tela-baixa:max-w-xl tela-baixa:p-5">
      <div className="text-muted-foreground mb-5 flex items-baseline justify-between font-mono text-xs xl:text-sm">
        <span className="text-foreground">3º ano A · Simulado 12</span>
        <span>{TURMA.length} alunos</span>
      </div>

      {/* Uma coluna pro nome + uma por questão. Grid e não tabela: é desenho,
          não dado — tabela de verdade entra no painel real */}
      <div className="grid grid-cols-[auto_repeat(12,minmax(0,1fr))] items-center gap-x-1 gap-y-1.5 xl:gap-y-2">
        <span />
        {Array.from({ length: QUESTOES }, (_, q) => (
          <span
            key={q}
            className={cn(
              "text-muted-foreground mx-auto flex size-6 items-center justify-center rounded-full font-mono text-xs xl:size-7 xl:text-sm",
              q === CRITICA && "bg-marca-texto text-sobre-marca-texto font-semibold",
            )}
          >
            {q + 1}
          </span>
        ))}

        {TURMA.map(({ aluno, respostas }) => (
          <Linha key={aluno} aluno={aluno} respostas={respostas} />
        ))}
      </div>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t pt-5">
        <p className="text-sm xl:text-base">
          <span className="bg-marca-texto text-sobre-marca-texto rounded px-1.5 py-0.5 font-mono text-xs font-semibold">
            Q{CRITICA + 1}
          </span>{" "}
          {ERRARAM_A_CRITICA} de {TURMA.length} erraram — revisar em aula
        </p>
        <div className="text-muted-foreground flex gap-4 font-mono text-xs xl:text-sm">
          <Legenda resultado="acertou">acertou</Legenda>
          <Legenda resultado="errou">errou</Legenda>
        </div>
      </div>
    </div>
  );
}

function Linha({ aluno, respostas }: { aluno: string; respostas: string }) {
  return (
    <>
      <span className="text-muted-foreground pr-4 text-sm xl:text-base">{aluno}</span>
      {[...respostas].map((r, q) => (
        <span
          key={q}
          className="flex justify-center py-1.5"
        >
          <span className={bolha({ resultado: r === "1" ? "acertou" : "errou", destaque: q === CRITICA })} />
        </span>
      ))}
    </>
  );
}

function Legenda({ resultado, children }: { resultado: "acertou" | "errou"; children: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span className={bolha({ resultado, tamanho: "legenda" })} />
      {children}
    </span>
  );
}
