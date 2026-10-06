"use client";

import { X } from "lucide-react";
import type { UseFormRegisterReturn } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export type ItemDaLista = {
  /** Chave estável do useFieldArray — o índice muda quando um item sai do meio. */
  chave: string;
  rotulo: string;
  questoes: number;
  campoDeQuantidade: UseFormRegisterReturn;
  erro?: string;
};

/**
 * As escolhas já feitas. Cada uma mostra quantas questões existe nos anos
 * escolhidos e aceita uma quantidade fixa — vazia, o sistema distribui ("auto").
 */
export function ListaDeEscolhas({ itens, aoRemover }: { itens: ItemDaLista[]; aoRemover: (indice: number) => void }) {
  if (itens.length === 0) {
    return (
      <p className="text-muted-foreground rounded-lg border border-dashed p-4 text-center text-sm">
        Nenhuma escolha ainda. Adicione áreas, matérias ou assuntos acima.
      </p>
    );
  }
  return (
    <ul className="grid gap-2" aria-label="Escolhas da prova">
      {itens.map((item, indice) => (
        <li key={item.chave}>
          <ItemDeEscolha item={item} aoRemover={() => aoRemover(indice)} />
        </li>
      ))}
    </ul>
  );
}

function ItemDeEscolha({ item, aoRemover }: { item: ItemDaLista; aoRemover: () => void }) {
  const idDoErro = `${item.chave}-erro`;
  return (
    <div className="grid gap-1 rounded-lg border p-3">
      <div className="flex items-center gap-2">
        {/* min-w-0: rótulo longo ("Eletrodinâmica: circuitos…") quebra em vez de alargar a tela */}
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium break-words">{item.rotulo}</p>
          <p className="text-muted-foreground text-xs">{item.questoes} questões nos anos escolhidos</p>
        </div>
        <Input
          type="number"
          inputMode="numeric"
          min={1}
          placeholder="auto"
          aria-label={`Quantidade de ${item.rotulo} (vazio = automático)`}
          aria-invalid={item.erro ? true : undefined}
          aria-describedby={item.erro ? idDoErro : undefined}
          className="h-11 w-20 text-center text-base"
          {...item.campoDeQuantidade}
        />
        <Button type="button" variant="ghost" size="icon-lg" aria-label={`Remover ${item.rotulo}`} onClick={aoRemover}>
          <X aria-hidden />
        </Button>
      </div>
      {item.erro && (
        <p id={idDoErro} role="alert" className="text-destructive text-sm">
          {item.erro}
        </p>
      )}
    </div>
  );
}
