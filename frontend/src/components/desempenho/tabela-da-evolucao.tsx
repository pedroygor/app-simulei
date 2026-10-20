import type { Coluna } from "@/lib/desempenho/evolucao";
import { rotuloDaSemana } from "@/lib/desempenho/evolucao";

/** Os mesmos números do gráfico em tabela, recolhida: leitor de tela e quem prefere ler. */
export function TabelaDaEvolucao({ colunas }: { colunas: Coluna[] }) {
  return (
    <details className="text-sm">
      <summary className="text-muted-foreground cursor-pointer py-1">Ver em números</summary>
      <table className="mt-2 w-full tabular-nums">
        <thead className="text-muted-foreground text-left text-xs">
          <tr>
            <th className="py-1 font-normal">Semana</th>
            <th className="py-1 text-right font-normal">Acerto</th>
            <th className="py-1 text-right font-normal">Respostas</th>
          </tr>
        </thead>
        <tbody>
          {colunas.map((c) => (
            <tr key={c.inicio} className="border-t">
              <td className="py-1">{rotuloDaSemana(c.inicio)}</td>
              <td className="py-1 text-right">{c.aproveitamento ? `${c.aproveitamento.percentual}%` : "—"}</td>
              <td className="py-1 text-right">{c.aproveitamento ? c.aproveitamento.respondidas : "sem prova"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}
