import { Button } from "@/components/ui/button";

/**
 * Rodapé fixo da montagem: o tamanho da prova sempre à vista e o botão onde o
 * polegar alcança. Os avisos que impedem o envio ficam aqui, colados no botão,
 * pra ninguém tocar num botão desabilitado sem saber por quê.
 */
export function BarraDeEnvio({
  total,
  escolhas,
  avisos,
  enviando,
}: {
  total: number | undefined;
  escolhas: number;
  avisos: string[];
  enviando: boolean;
}) {
  return (
    <div className="bg-background/95 sticky bottom-0 -mx-4 mt-6 grid gap-2 border-t px-4 py-3 backdrop-blur">
      {avisos.map((aviso) => (
        <p key={aviso} role="alert" className="text-destructive text-sm">
          {aviso}
        </p>
      ))}
      <p className="text-muted-foreground text-sm tabular-nums" aria-live="polite">
        {Number.isInteger(total) ? `${total} questões` : "—"} · {escolhas} {escolhas === 1 ? "escolha" : "escolhas"}
      </p>
      <Button type="submit" size="xl" className="w-full" disabled={enviando || avisos.length > 0 || escolhas === 0}>
        {enviando ? "Montando…" : "Montar prova"}
      </Button>
    </div>
  );
}
