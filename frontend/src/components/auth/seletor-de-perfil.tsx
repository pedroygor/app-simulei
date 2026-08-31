"use client";

import { GraduationCap, Users } from "lucide-react";
import type { Role } from "@/lib/api/tipos";
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
 * Usa radiogroup de verdade (não dois botões com onClick): navegação por setas
 * e anúncio correto no leitor de tela vêm do papel ARIA, não da aparência. É a
 * mesma regra que vale pras bolhas de alternativa.
 */
export function SeletorDePerfil({ valor, aoMudar }: SeletorDePerfilProps) {
  return (
    <div role="radiogroup" aria-label="Tipo de conta" className="grid grid-cols-2 gap-2">
      {PERFIS.map(({ valor: perfil, rotulo, Icone }) => {
        const selecionado = valor === perfil;
        return (
          <button
            key={perfil}
            type="button"
            role="radio"
            aria-checked={selecionado}
            onClick={() => aoMudar(perfil)}
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
