import { cva } from "class-variance-authority";
import type { Area } from "@/lib/api/questoes";
import { NOME_DA_AREA } from "@/lib/questoes/filtro-da-url";
import { cn } from "@/lib/utils";

/**
 * Cor por área, com CVA em vez de ternário aninhado (regra 8.5).
 *
 * A cor é reforço, NUNCA o sinal único: o nome da área vem escrito ao lado.
 * Mesma regra que vale pro seletor de perfil — quem não distingue as cores
 * precisa da informação mesmo assim.
 */
const etiqueta = cva("inline-block size-2 shrink-0 rounded-full", {
  variants: {
    area: {
      LINGUAGENS: "bg-area-linguagens",
      CIENCIAS_HUMANAS: "bg-area-humanas",
      CIENCIAS_NATUREZA: "bg-area-natureza",
      MATEMATICA: "bg-area-matematica",
    },
  },
});

export function EtiquetaDeArea({ area, className }: { area: Area; className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-1.5 text-xs", className)}>
      <span className={etiqueta({ area })} aria-hidden />
      {NOME_DA_AREA[area]}
    </span>
  );
}
