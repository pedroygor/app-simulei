import { AlarmClock } from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * O que fica no lugar da questão quando o cronômetro zera: nada mais pode ser
 * marcado, e a prova está sendo entregue sozinha. role="status": o leitor de
 * tela anuncia sem roubar o foco.
 */
export function TempoEsgotado({ erro, aoTentarDeNovo }: { erro: string | null; aoTentarDeNovo: () => void }) {
  return (
    <section role="status" className="grid justify-items-center gap-3 rounded-lg border p-6 text-center">
      <AlarmClock aria-hidden className="text-destructive size-8" />
      <h2 className="font-heading text-xl font-bold">Tempo esgotado</h2>
      {erro ? (
        <>
          <p className="text-destructive text-sm">Não foi possível entregar: {erro}</p>
          <Button type="button" onClick={aoTentarDeNovo}>
            Tentar entregar de novo
          </Button>
        </>
      ) : (
        <p className="text-muted-foreground text-sm">Entregando sua prova…</p>
      )}
    </section>
  );
}
