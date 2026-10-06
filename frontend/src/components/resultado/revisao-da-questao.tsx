"use client";

import { cva } from "class-variance-authority";
import { useEffect, useRef } from "react";
import { AlternativaBolha } from "@/components/questao/alternativa-bolha";
import { EtiquetaDeArea } from "@/components/questao/etiqueta-de-area";
import { EtiquetaDeAssunto } from "@/components/questao/etiqueta-de-assunto";
import { TextoDaQuestao } from "@/components/questao/texto-da-questao";
import type { QuestaoCorrigida } from "@/lib/api/resultados";
import { estadoNaRevisao, situacaoNaRevisao, type SituacaoNaRevisao } from "@/lib/prova/correcao";

const selo = cva("text-xs font-semibold tracking-wide uppercase", {
  variants: {
    situacao: { acertou: "text-primary", errou: "text-destructive", emBranco: "text-muted-foreground" },
  },
});

const DITO: Record<SituacaoNaRevisao, string> = {
  acertou: "Você acertou",
  errou: "Você errou",
  emBranco: "Em branco",
};

/**
 * Uma questão corrigida. Lista de leitura, não radiogroup: nada mais se marca.
 * Montada de novo a cada troca (key no pai): o foco vai pro título, como na prova.
 */
export function RevisaoDaQuestao({ questao, total }: { questao: QuestaoCorrigida; total: number }) {
  const titulo = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    titulo.current?.focus();
  }, []);

  const situacao = situacaoNaRevisao(questao);

  return (
    <article className="grid gap-5">
      <header className="grid gap-2">
        <h2 ref={titulo} tabIndex={-1} className="font-heading text-xl font-bold outline-none">
          Questão {questao.ordem} <span className="text-muted-foreground font-normal">de {total}</span>
        </h2>
        <p className={selo({ situacao })}>{DITO[situacao]}</p>
        <div className="text-muted-foreground flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
          <EtiquetaDeArea area={questao.area} />
          <span>
            ENEM {questao.ano} · questão {questao.numero}
          </span>
          {/* Depois de entregar o assunto não dá mais dica: explica o que errou */}
          <EtiquetaDeAssunto materia={questao.materia} assunto={questao.assunto} comMateria />
        </div>
      </header>

      <TextoDaQuestao texto={questao.enunciado} />
      <TextoDaQuestao texto={questao.comando} className="font-medium" />

      <ol aria-label="Alternativas" className="grid gap-2">
        {questao.alternativas.map((alternativa) => (
          <li key={alternativa.id}>
            <AlternativaBolha alternativa={alternativa} estado={estadoNaRevisao(alternativa, questao)} />
          </li>
        ))}
      </ol>
    </article>
  );
}
