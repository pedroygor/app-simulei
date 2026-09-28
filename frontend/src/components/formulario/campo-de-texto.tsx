"use client";

import { CircleX } from "lucide-react";
import { useId } from "react";
import type { ComponentProps, ReactNode } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

type CampoDeTextoProps = ComponentProps<typeof Input> & {
  rotulo: string;
  erro?: string;
  /** Texto de apoio abaixo do campo (ex: regra de senha). */
  ajuda?: string;
  /** Controle dentro do campo, à direita (ex: mostrar senha). */
  acessorio?: ReactNode;
};

/**
 * Rótulo + campo + mensagem de erro, com a fiação de acessibilidade pronta.
 *
 * Existe pra tela não repetir essa amarração em todo campo: o `id` que liga
 * label e input, o `aria-invalid`, e o `aria-describedby` apontando pra
 * mensagem certa. Errar isso é fácil e o leitor de tela cala.
 */
export function CampoDeTexto({ rotulo, erro, ajuda, acessorio, className, ...props }: CampoDeTextoProps) {
  // useId gera identificador estável entre servidor e cliente
  const idGerado = useId();
  const id = props.id ?? idGerado;
  const idDoErro = `${id}-erro`;
  const idDaAjuda = `${id}-ajuda`;

  return (
    <div className="grid gap-2">
      <Label htmlFor={id}>{rotulo}</Label>
      <div className="relative">
        <Input
          id={id}
          aria-invalid={erro ? true : undefined}
          aria-describedby={cn(erro && idDoErro, ajuda && idDaAjuda) || undefined}
          // 48px: campo é controle de aluno, e a regra de 44px do CSS base só
          // cobre botão e radio. text-base (16px) também evita o zoom
          // automático do Safari no iPhone ao focar o campo
          className={cn("h-12 px-3.5 text-base md:text-base", acessorio && "pr-14", className)}
          {...props}
        />
        {acessorio && <div className="absolute inset-y-0 right-1 flex items-center">{acessorio}</div>}
      </div>
      {ajuda && !erro && (
        <p id={idDaAjuda} className="text-muted-foreground text-xs">
          {ajuda}
        </p>
      )}
      {/* role="alert" faz o leitor de tela anunciar assim que a mensagem surge */}
      {erro && (
        <p id={idDoErro} role="alert" className="text-destructive flex items-center gap-1.5 text-sm">
          <CircleX aria-hidden className="size-4 shrink-0" />
          {erro}
        </p>
      )}
    </div>
  );
}
