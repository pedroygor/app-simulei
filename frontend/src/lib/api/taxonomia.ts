import { requisitar } from "./cliente";
import type { Area } from "./questoes";

/** Espelham AreaDaTaxonomiaResponse do backend: área → matéria → assunto. */

export type AssuntoDaTaxonomia = {
  slug: string;
  nome: string;
  /** Questões disponíveis (nem anuladas nem incompletas), nos anos pedidos ou no banco inteiro. */
  questoes: number;
};

export type MateriaDaTaxonomia = {
  slug: string;
  nome: string;
  assuntos: AssuntoDaTaxonomia[];
};

export type AreaDaTaxonomia = {
  area: Area;
  materias: MateriaDaTaxonomia[];
};

export type Taxonomia = AreaDaTaxonomia[];

/**
 * Sem anos, conta o banco inteiro (filtros da listagem). Com anos, conta só as
 * questões desses anos: é o número que a montagem de prova pode sortear.
 */
export function buscarTaxonomia(anos: number[] = []): Promise<Taxonomia> {
  const parametros = new URLSearchParams();
  for (const ano of anos) parametros.append("anos", String(ano));
  const consulta = parametros.toString();
  return requisitar<Taxonomia>(consulta ? `/taxonomia?${consulta}` : "/taxonomia");
}

/**
 * Matérias oferecidas no filtro: as da área escolhida, ou todas quando não há
 * área (14 opções — cabe numa lista). A ordem é a da taxonomia, que o backend
 * já entrega na sequência de estudo.
 */
export function materiasDaArea(taxonomia: Taxonomia | undefined, area: Area | undefined): MateriaDaTaxonomia[] {
  if (!taxonomia) return [];
  return taxonomia.filter((a) => area === undefined || a.area === area).flatMap((a) => a.materias);
}

/**
 * Assuntos da matéria escolhida. Sem matéria, nenhum: 118 assuntos soltos
 * numa lista de celular ninguém consegue usar.
 */
export function assuntosDaMateria(
  taxonomia: Taxonomia | undefined,
  materia: string | undefined,
): AssuntoDaTaxonomia[] {
  if (!taxonomia || !materia) return [];
  return materiasDaArea(taxonomia, undefined).find((m) => m.slug === materia)?.assuntos ?? [];
}

/** A área a que a matéria pertence — escolher "Física" já diz "Natureza". */
export function areaDaMateria(taxonomia: Taxonomia | undefined, materia: string): Area | undefined {
  return taxonomia?.find((a) => a.materias.some((m) => m.slug === materia))?.area;
}
