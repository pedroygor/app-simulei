"use client";

import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { OpcaoDeFiltro } from "@/lib/questoes/filtro-da-url";

/**
 * O Select do Radix não representa "sem filtro" com value="", porque string
 * vazia é o jeito dele dizer "nada selecionado". Então usamos um valor
 * sentinela e traduzimos na borda.
 */
const TODOS = "__todos__";

/**
 * Rótulo + Select com uma opção de "nenhum" no topo. Usado nos filtros da
 * listagem e na cascata área → matéria → assunto da montagem de prova.
 */
export function CampoDeSelecao({
  id,
  rotulo,
  valor,
  aoMudar,
  opcoes,
  rotuloDeTodos,
  desabilitado = false,
}: {
  id: string;
  rotulo: string;
  valor: string | undefined;
  aoMudar: (valor: string | undefined) => void;
  opcoes: OpcaoDeFiltro[];
  rotuloDeTodos: string;
  desabilitado?: boolean;
}) {
  return (
    // min-w-0: item de grid não encolhe abaixo do conteúdo por padrão. Sem
    // isto, "Eletrodinâmica: circuitos, potência e consumo elétrico" alargava
    // a coluna e a página ganhava rolagem lateral a 360px (medido: 404px)
    <div className="grid min-w-0 gap-1.5">
      <Label htmlFor={id}>{rotulo}</Label>
      <Select
        value={valor ?? TODOS}
        onValueChange={(novo) => aoMudar(novo === TODOS ? undefined : novo)}
        disabled={desabilitado}
      >
        <SelectTrigger id={id} className="w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={TODOS}>{rotuloDeTodos}</SelectItem>
          {opcoes.map((opcao) => (
            <SelectItem key={opcao.valor} value={opcao.valor}>
              {opcao.rotulo}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
