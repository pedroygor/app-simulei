"use client";

import { cva } from "class-variance-authority";
import { useState } from "react";
import { type Coluna, leituraDaColuna, rotuloDaSemana, ultimaComDado } from "@/lib/desempenho/evolucao";

// Altura do gráfico: o geral maior, os por área menores (pequenos múltiplos)
const area = cva("relative grid grid-flow-col auto-cols-fr items-end", {
  variants: { tamanho: { grande: "h-28", pequeno: "h-16" } },
});

// Coluna medida = cheia; poucas respostas = vazada (só contorno), dita também em texto
const barra = cva("w-full max-w-6 rounded-t-sm", {
  variants: {
    estado: {
      medida: "bg-primary",
      pouca: "border-primary border-2 border-b-0",
    },
  },
});

// Rótulo da semana: o da coluna escolhida ganha destaque — no celular, é a confirmação
// de onde o dedo tocou
const rotulo = cva("", {
  variants: { ativa: { true: "text-foreground font-medium", false: "text-muted-foreground" } },
});

/**
 * Colunas por semana, de 0 a 100% (escala FIXA: com escala própria, 60% numa área
 * pareceria tão alto quanto 90% em outra). Uma série só, então sem legenda: o título
 * diz o que é. Semana sem prova = espaço vazio, nunca uma coluna de 0%.
 *
 * Em vez de um número em cada coluna, uma LEITURA acima mostra a semana mais recente
 * com dado e troca quando o aluno toca, passa o mouse ou foca uma coluna — no celular
 * não existe hover. Cada coluna é um botão com a frase completa no nome acessível.
 */
export function GraficoSemanal({
  titulo,
  colunas,
  tamanho,
}: {
  titulo: string;
  colunas: Coluna[];
  tamanho: "grande" | "pequeno";
}) {
  const [ativa, setAtiva] = useState<number | null>(null);
  const mostrada = ativa ?? ultimaComDado(colunas);

  return (
    <figure className="grid gap-2">
      <figcaption className="grid gap-0.5">
        <span className="text-sm font-medium">{titulo}</span>
        {/* Sem aria-live: cada coluna já é um botão com esta frase no nome, e o anúncio
            sairia dobrado a cada foco (e sem parar, com o mouse passando por cima) */}
        <span className="text-muted-foreground min-h-5 text-sm tabular-nums">
          {mostrada === null ? "Nenhuma prova entregue nestas semanas" : leituraDaColuna(colunas[mostrada])}
        </span>
      </figcaption>
      <div className="grid grid-cols-[auto_1fr] gap-2">
        <EixoDePercentual />
        <div className={area({ tamanho })} onMouseLeave={() => setAtiva(null)}>
          <GradeDoGrafico />
          {colunas.map((coluna, i) => (
            <ColunaDaSemana
              key={coluna.inicio}
              coluna={coluna}
              ativa={mostrada === i}
              aoAtivar={() => setAtiva(i)}
            />
          ))}
        </div>
        <span />
        <div aria-hidden className="grid grid-flow-col auto-cols-fr text-center text-xs tabular-nums">
          {colunas.map((c, i) => (
            <span key={c.inicio} className={rotulo({ ativa: mostrada === i })}>
              {rotuloDaSemana(c.inicio)}
            </span>
          ))}
        </div>
      </div>
    </figure>
  );
}

function ColunaDaSemana({ coluna, ativa, aoAtivar }: { coluna: Coluna; ativa: boolean; aoAtivar: () => void }) {
  const a = coluna.aproveitamento;
  return (
    // O botão ocupa a altura toda: alvo de toque grande mesmo com coluna baixa ou vazia
    <button
      type="button"
      aria-label={leituraDaColuna(coluna)}
      aria-pressed={ativa}
      onClick={aoAtivar}
      onFocus={aoAtivar}
      onMouseEnter={aoAtivar}
      className="relative flex h-full items-end justify-center"
    >
      {a && (
        <span
          aria-hidden
          className={barra({ estado: a.amostraSuficiente ? "medida" : "pouca" })}
          // height inline: é DADO (o percentual), não estilo. Mínimo de 2px pra 0% ainda
          // aparecer como "respondeu e errou tudo", diferente da semana vazia
          style={{ height: `max(2px, ${a.percentual ?? 0}%)` }}
        />
      )}
    </button>
  );
}

/** 100, 50 e 0% à esquerda: a escala é fixa, então os rótulos também. */
function EixoDePercentual() {
  return (
    <div aria-hidden className="text-muted-foreground flex flex-col justify-between text-right text-xs tabular-nums">
      <span>100%</span>
      <span>50%</span>
      <span>0%</span>
    </div>
  );
}

/** Linhas finas e contínuas em 0, 50 e 100%: recessivas, atrás das colunas. */
function GradeDoGrafico() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 flex flex-col justify-between">
      <span className="border-border border-t" />
      <span className="border-border border-t" />
      <span className="border-border border-t" />
    </div>
  );
}
