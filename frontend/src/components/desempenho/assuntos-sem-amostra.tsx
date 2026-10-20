import type { DesempenhoNoAssunto } from "@/lib/api/desempenho";
import { faltamRespostas, placarCurto } from "@/lib/desempenho/desempenho";
import { LinkDoAssunto } from "./link-do-assunto";

/**
 * Os assuntos que ainda não dá pra medir, recolhidos: com 2 respostas um chute
 * certo é 50%, então eles aparecem sem barra e sem destaque. <details> nativo:
 * abre e fecha com teclado e leitor de tela sem JavaScript nosso.
 */
export function AssuntosSemAmostra({ assuntos, amostraMinima }: { assuntos: DesempenhoNoAssunto[]; amostraMinima: number }) {
  if (assuntos.length === 0) return null;
  return (
    <details className="text-sm">
      <summary className="text-muted-foreground cursor-pointer py-1">
        {assuntos.length === 1 ? "1 assunto" : `${assuntos.length} assuntos`} ainda sem respostas suficientes
      </summary>
      <p className="text-muted-foreground mt-1 text-xs">
        Um assunto é medido a partir de {amostraMinima} questões respondidas.
      </p>
      <ul className="mt-2 grid gap-2">
        {assuntos.map((a) => (
          <li key={a.assunto.slug} className="flex items-baseline justify-between gap-3">
            <span className="min-w-0 break-words">
              <LinkDoAssunto assunto={a.assunto} />
              <span className="text-muted-foreground"> · faltam {faltamRespostas(a.aproveitamento, amostraMinima)}</span>
            </span>
            <span className="text-muted-foreground shrink-0 tabular-nums">{placarCurto(a.aproveitamento)}</span>
          </li>
        ))}
      </ul>
    </details>
  );
}
