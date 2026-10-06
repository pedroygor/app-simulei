import { cva } from "class-variance-authority";
import type { SituacaoDaProva } from "@/lib/api/simulados";

// A situação dita em TEXTO; a cor só reforça. Em andamento é a que pede ação
// (continuar), por isso o destaque
const selo = cva("inline-flex shrink-0 items-center rounded-full border px-2 py-0.5 text-xs font-medium", {
  variants: {
    situacao: {
      NAO_INICIADA: "border-contorno text-muted-foreground",
      EM_ANDAMENTO: "border-primary text-primary",
      ENTREGUE: "border-border text-foreground",
    },
  },
});

const TEXTO: Record<SituacaoDaProva, string> = {
  NAO_INICIADA: "Não iniciada",
  EM_ANDAMENTO: "Em andamento",
  ENTREGUE: "Entregue",
};

export function SeloDaSituacao({ situacao }: { situacao: SituacaoDaProva }) {
  return <span className={selo({ situacao })}>{TEXTO[situacao]}</span>;
}
