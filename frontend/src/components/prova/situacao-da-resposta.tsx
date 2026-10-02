import { Button } from "@/components/ui/button";
import type { SituacaoDoSalvamento } from "@/lib/prova/fila-de-salvamento";

/**
 * Diz se a resposta chegou ao servidor. No wi-fi do cursinho isso importa: o
 * aluno precisa saber que tocar não basta se a rede caiu. aria-live: o leitor de
 * tela anuncia a mudança sem o aluno ter que procurar.
 */
export function SituacaoDaResposta({
  situacao,
  respondida,
  aoTentarDeNovo,
}: {
  situacao: SituacaoDoSalvamento;
  respondida: boolean;
  aoTentarDeNovo: () => void;
}) {
  return (
    <div aria-live="polite" className="text-muted-foreground min-h-8 text-sm">
      {situacao === "salvando" && <p>Salvando…</p>}
      {situacao === "salva" && respondida && <p>Resposta salva</p>}
      {situacao === "erro" && (
        <div className="flex flex-wrap items-center gap-2">
          <p role="alert" className="text-destructive">
            Não foi salva. Confira a conexão.
          </p>
          <Button type="button" variant="outline" size="sm" onClick={aoTentarDeNovo}>
            Tentar de novo
          </Button>
        </div>
      )}
    </div>
  );
}
