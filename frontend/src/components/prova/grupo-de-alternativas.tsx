"use client";

import { useRef, type KeyboardEvent } from "react";
import { AlternativaBolha } from "@/components/questao/alternativa-bolha";
import type { Alternativa } from "@/lib/api/questoes";
import { indiceDestino, indiceFocavel } from "@/lib/prova/teclado";

/**
 * As alternativas de uma questão da prova, como radiogroup de verdade (regra de
 * acessibilidade da identidade visual): Tab entra e sai do grupo numa tecla só,
 * setas trocam de opção, Espaço/Enter marcam a que está em foco.
 *
 * As setas MARCAM, não só movem o foco — é o comportamento do radio nativo e o
 * que o padrão do WAI-ARIA pede. Cada seta vira um salvamento; quem coordena os
 * salvamentos é a tela da prova, não este componente.
 */
export function GrupoDeAlternativas({
  alternativas,
  marcada,
  aoMarcar,
  rotuladoPor,
}: {
  alternativas: Alternativa[];
  /** Id da alternativa marcada; null = em branco. */
  marcada: number | null;
  aoMarcar: (alternativaId: number) => void;
  /** Id do elemento que enuncia a pergunta: o nome do grupo pro leitor de tela. */
  rotuladoPor: string;
}) {
  const opcoes = useRef<(HTMLDivElement | null)[]>([]);
  const focavel = indiceFocavel(
    alternativas.map((a) => a.id),
    marcada,
  );

  function aoTeclar(indice: number, evento: KeyboardEvent<HTMLDivElement>) {
    if (evento.key === " " || evento.key === "Enter") {
      // Sem preventDefault, o Espaço também rolaria a página
      evento.preventDefault();
      aoMarcar(alternativas[indice].id);
      return;
    }
    const destino = indiceDestino(indice, evento.key, alternativas.length);
    if (destino === null) return;
    evento.preventDefault();
    opcoes.current[destino]?.focus();
    aoMarcar(alternativas[destino].id);
  }

  return (
    <div role="radiogroup" aria-labelledby={rotuladoPor} className="grid gap-2">
      {alternativas.map((alternativa, indice) => (
        <AlternativaBolha
          key={alternativa.id}
          alternativa={alternativa}
          estado={alternativa.id === marcada ? "selecionada" : "neutra"}
          radio={{
            focavel: indice === focavel,
            aoEscolher: () => aoMarcar(alternativa.id),
            aoTeclar: (evento) => aoTeclar(indice, evento),
          }}
          ref={(elemento) => {
            opcoes.current[indice] = elemento;
          }}
        />
      ))}
    </div>
  );
}
