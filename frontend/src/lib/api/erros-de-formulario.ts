import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { ErroDaApi } from "./erros";

/**
 * Leva os erros de campo do backend para o formulário.
 *
 * O zod valida no cliente, mas o backend valida de novo (e é ele quem manda:
 * o cliente é contornável). Quando o 400 vem com a lista `campos[]`, a
 * mensagem precisa aparecer NO campo, não num alerta genérico no topo —
 * senão o usuário não sabe o que corrigir.
 *
 * Devolve true quando conseguiu direcionar tudo; false quando o erro é geral
 * e cabe ao chamador mostrá-lo de outro jeito.
 */
export function aplicarErrosDeCampo<T extends FieldValues>(
  erro: unknown,
  setError: UseFormSetError<T>,
  camposConhecidos: readonly Path<T>[],
): boolean {
  if (!(erro instanceof ErroDaApi) || !erro.temErrosDeCampo) {
    return false;
  }
  let algumDirecionado = false;
  for (const campo of erro.campos) {
    // Só direciona o que existe no formulário: um campo desconhecido vindo do
    // servidor viraria erro invisível, preso num campo que ninguém renderiza
    if (camposConhecidos.includes(campo.campo as Path<T>)) {
      setError(campo.campo as Path<T>, { type: "server", message: campo.mensagem });
      algumDirecionado = true;
    }
  }
  return algumDirecionado;
}
