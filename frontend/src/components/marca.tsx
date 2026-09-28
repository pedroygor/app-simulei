import { cn } from "@/lib/utils";

/** Logotipo: a bolha de marca-texto + o nome. A bolha é a assinatura visual. */
export function Marca({ className }: { className?: string }) {
  return (
    <span className={cn("font-heading inline-flex items-center gap-2.5 text-xl font-semibold tracking-tight", className)}>
      <span aria-hidden className="bg-marca-texto size-4 rounded-full" />
      Simulei
    </span>
  );
}
