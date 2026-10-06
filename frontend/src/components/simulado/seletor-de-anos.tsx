"use client";

import { useId } from "react";
import { Button } from "@/components/ui/button";

/**
 * Os anos que entram no sorteio, como botões de alternância. Nenhum marcado =
 * todos os anos — o padrão, e o que o "Todos" volta a ser.
 *
 * Os anos vêm dos DADOS (GET /questoes/anos), não de constante: só aparece ano
 * que tem questão, então não há como pedir 2024 antes de a prova ser importada.
 *
 * aria-pressed: cada ano é um botão liga/desliga, e o leitor de tela precisa
 * dizer o estado. O selecionado muda cor E preenchimento (variante), nunca só cor.
 */
export function SeletorDeAnos({
  disponiveis,
  selecionados,
  aoMudar,
}: {
  disponiveis: number[];
  selecionados: number[];
  aoMudar: (anos: number[]) => void;
}) {
  const idDoRotulo = useId();
  const todos = selecionados.length === 0;

  function alternar(ano: number) {
    aoMudar(selecionados.includes(ano) ? selecionados.filter((a) => a !== ano) : [...selecionados, ano]);
  }

  return (
    <fieldset className="grid gap-2">
      <legend id={idDoRotulo} className="mb-2 text-sm font-medium">
        Anos
      </legend>
      <div role="group" aria-labelledby={idDoRotulo} className="flex flex-wrap gap-2">
        <Button type="button" size="sm" variant={todos ? "default" : "outline"} aria-pressed={todos} onClick={() => aoMudar([])}>
          Todos
        </Button>
        {disponiveis.map((ano) => {
          const marcado = selecionados.includes(ano);
          return (
            <Button
              key={ano}
              type="button"
              size="sm"
              variant={marcado ? "default" : "outline"}
              aria-pressed={marcado}
              onClick={() => alternar(ano)}
            >
              {ano}
            </Button>
          );
        })}
      </div>
    </fieldset>
  );
}
