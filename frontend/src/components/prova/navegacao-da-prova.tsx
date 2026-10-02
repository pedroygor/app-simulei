import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * Rodapé fixo da prova: anterior e próxima onde o polegar alcança. Na última
 * questão, "Próxima" vira "Entregar" — o fim natural da prova é ali.
 */
export function NavegacaoDaProva({
  indice,
  total,
  aoIr,
  aoEntregar,
}: {
  indice: number;
  total: number;
  aoIr: (indice: number) => void;
  aoEntregar: () => void;
}) {
  const ultima = indice === total - 1;

  return (
    <nav aria-label="Navegação da prova" className="bg-background/95 sticky bottom-0 -mx-4 border-t px-4 py-3 backdrop-blur">
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
