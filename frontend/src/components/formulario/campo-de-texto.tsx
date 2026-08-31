"use client";

import { useId } from "react";
import type { ComponentProps } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

type CampoDeTextoProps = ComponentProps<typeof Input> & {
  rotulo: string;
  erro?: string;
  /** Texto de apoio abaixo do campo (ex: regra de senha). */
  ajuda?: string;
};

/**
 * Rótulo + campo + mensagem de erro, com a fiação de acessibilidade pronta.
 *
 * Existe pra tela não repetir essa amarração em todo campo: o `id` que liga
 * label e input, o `aria-invalid`, e o `aria-describedby` apontando pra
 * mensagem certa. Errar isso é fácil e o leitor de tela cala.
 */
export function CampoDeTexto({ rotulo, erro, ajuda, className, ...props }: CampoDeTextoProps) {
  // useId gera identificador estável entre servidor e cliente
  const idGerado = useId();
  const id = props.id ?? idGerado;
  const idDoErro = `${id}-erro`;
  const idDaAjuda = `${id}-ajuda`;

  return (
    <div className="grid gap-2">
      <Label htmlFor={id}>{rotulo}</Label>
      <Input
        id={id}
        aria-invalid={erro ? true : undefined}
        aria-describedby={cn(erro && idDoErro, ajuda && idDaAjuda) || undefined}
        className={className}
        {...props}
      />
      {ajuda && !erro && (
        <p id={idDaAjuda} className="text-muted-foreground text-xs">
          {ajuda}
        </p>
      )}
      {/* role="alert" faz o leitor de tela anunciar assim que a mensagem surge */}
      {erro && (
        <p id={idDoErro} role="alert" className="text-destructive text-xs">
          {erro}
        </p>
      )}
    </div>
  );
}
