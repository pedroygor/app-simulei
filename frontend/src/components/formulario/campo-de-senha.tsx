"use client";

import { Eye, EyeOff } from "lucide-react";
import { useState, type ComponentProps } from "react";
import { CampoDeTexto } from "./campo-de-texto";

/**
 * Campo de senha com botão de mostrar/ocultar.
 *
 * No celular, digitar senha às cegas num teclado pequeno é a maior causa de
 * "senha errada" que não é senha errada.
 *
 * Acessibilidade do botão: nome FIXO ("Mostrar senha") + aria-pressed. Trocar
 * o nome a cada clique ("mostrar" → "ocultar") junto com o estado faz o leitor
 * de tela anunciar as duas coisas e confundir; o padrão de botão de alternância
 * é um nome só e o estado dizendo se está ligado.
 */
export function CampoDeSenha(props: Omit<ComponentProps<typeof CampoDeTexto>, "type" | "acessorio">) {
  const [visivel, setVisivel] = useState(false);
  const Icone = visivel ? EyeOff : Eye;

  return (
    <CampoDeTexto
      {...props}
      type={visivel ? "text" : "password"}
      acessorio={
        <button
          type="button"
          aria-label="Mostrar senha"
          aria-pressed={visivel}
          onClick={() => setVisivel((v) => !v)}
          // size-11 = 44px: alvo mínimo de toque, mesmo dentro do campo
          className="text-muted-foreground hover:text-foreground focus-visible:ring-ring flex size-11 items-center justify-center rounded-md focus-visible:ring-2 focus-visible:outline-none"
        >
          <Icone aria-hidden className="size-5" />
        </button>
      }
    />
  );
}
