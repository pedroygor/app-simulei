"use client";

import { GraduationCap, Users } from "lucide-react";
import { useRef, type KeyboardEvent } from "react";
import type { Role } from "@/lib/api/tipos";
import { indiceDestino } from "@/lib/prova/teclado";
import { cn } from "@/lib/utils";

const PERFIS = [
  { valor: "ALUNO", rotulo: "Sou aluno", Icone: GraduationCap },
  { valor: "PROFESSOR", rotulo: "Sou professor", Icone: Users },
] as const satisfies readonly { valor: Role; rotulo: string; Icone: typeof Users }[];

type SeletorDePerfilProps = {
  valor: Role;
  aoMudar: (role: Role) => void;
};

/**
 * Escolha entre aluno e professor.
 *
 * Usa radiogroup de verdade (não dois botões com onClick): o anúncio no leitor
 * de tela vem do papel ARIA, não da aparência. O TECLADO, porém, o papel não dá
 * de graça: roving tabindex (só o marcado entra no Tab) e setas que trocam e
 * marcam, no padrão WAI-ARIA — o mesmo `teclado.ts` das bolhas da prova.
 */
export function SeletorDePerfil({ valor, aoMudar }: SeletorDePerfilProps) {
  const opcoes = useRef<(HTMLButtonElement | null)[]>([]);

  function aoTeclar(indice: number, evento: KeyboardEvent<HTMLButtonElement>) {
    const destino = indiceDestino(indice, evento.key, PERFIS.length);
    if (destino === null) return;
    // Sem preventDefault, a seta também rolaria a página
    evento.preventDefault();
    opcoes.current[destino]?.focus();
    aoMudar(PERFIS[destino].valor);
  }

  return (
    <div role="radiogroup" aria-label="Tipo de conta" className="grid grid-cols-2 gap-2">
      {PERFIS.map(({ valor: perfil, rotulo, Icone }, indice) => {
        const selecionado = valor === perfil;
        return (
          <button
            key={perfil}
            ref={(elemento) => {
              opcoes.current[indice] = elemento;
            }}
            type="button"
            role="radio"
            aria-checked={selecionado}
            // Sempre há um perfil marcado (o formulário começa em ALUNO), então
            // o marcado é sempre o ponto de entrada do Tab
            tabIndex={selecionado ? 0 : -1}
            onClick={() => aoMudar(perfil)}
            onKeyDown={(evento) => aoTeclar(indice, evento)}
            className={cn(
              "flex flex-col items-center gap-1.5 rounded-lg border p-3 text-sm transition-colors",
              "focus-visible:ring-ring focus-visible:ring-2 focus-visible:outline-none",
              selecionado
                ? // Selecionado muda cor E ganha peso: cor não pode ser o
                  // único sinal (~8% dos meninos não distinguem bem)
                  "border-primary bg-primary/10 text-foreground font-medium"
                : "border-contorno text-muted-foreground hover:text-foreground",
            )}
          >
            <Icone className="size-5" aria-hidden />
            {rotulo}
          </button>
        );
      })}
    </div>
  );
}
