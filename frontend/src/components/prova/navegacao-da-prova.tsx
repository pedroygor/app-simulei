import { ChevronLeft, ChevronRight } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";

/**
 * Rodapé fixo da prova: anterior e próxima onde o polegar alcança. Na última
 * questão, "Próxima" vira "Entregar" — o fim natural da prova é ali.
 *
 * O cronômetro mora AQUI, não no cabeçalho: o cabeçalho rola junto com a
 * questão, e num texto-base longo o aluno passaria a prova sem ver o tempo
 * (visto em 360px, 14/10/2026). O rodapé é fixo e não remonta entre questões.
 */
export function NavegacaoDaProva({
  indice,
  total,
  aoIr,
  aoEntregar,
  cronometro,
}: {
  indice: number;
  total: number;
  aoIr: (indice: number) => void;
  aoEntregar: () => void;
  /** Só em prova com tempo-limite. */
  cronometro?: ReactNode;
}) {
  const ultima = indice === total - 1;

  return (
    <nav aria-label="Navegação da prova" className="bg-background/95 sticky bottom-0 -mx-4 border-t px-4 py-3 backdrop-blur">
      {cronometro && <div className="mb-2 flex justify-center">{cronometro}</div>}
      <div className="grid grid-cols-2 gap-2">
        <Button type="button" variant="outline" size="xl" disabled={indice === 0} onClick={() => aoIr(indice - 1)}>
          <ChevronLeft aria-hidden />
          Anterior
        </Button>
        {ultima ? (
          <Button type="button" size="xl" onClick={aoEntregar}>
            Entregar
          </Button>
        ) : (
          <Button type="button" size="xl" onClick={() => aoIr(indice + 1)}>
            Próxima
            <ChevronRight aria-hidden />
          </Button>
        )}
      </div>
    </nav>
  );
}
