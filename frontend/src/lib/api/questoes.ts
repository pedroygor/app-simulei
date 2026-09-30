import { requisitar } from "./cliente";

/** Espelham QuestaoResumoResponse / QuestaoDetalheResponse do backend. */

export const AREAS = [
  "LINGUAGENS",
  "CIENCIAS_HUMANAS",
  "CIENCIAS_NATUREZA",
  "MATEMATICA",
] as const;
export type Area = (typeof AREAS)[number];

export const IDIOMAS = ["INGLES", "ESPANHOL"] as const;
export type Idioma = (typeof IDIOMAS)[number];

/**
 * Matéria ou assunto da taxonomia: o slug identifica (vai pro filtro e pra
 * URL), o nome é pra exibir. Espelha RotuloResponse do backend.
 */
export type Rotulo = { slug: string; nome: string };

export type QuestaoResumo = {
  id: number;
  ano: number;
  numero: number;
  area: Area;
  idioma: Idioma | null;
  /** null = questão ainda não classificada. */
  materia: Rotulo | null;
  assunto: Rotulo | null;
  /**
   * Prévia cortada no BANCO por substring — pode terminar no meio de um
   * `![](...)`. Medido: 9 das 182 questões de 2023 cortam dentro da sintaxe.
   * Por isso é tratada como TEXTO PURO na tela, nunca renderizada como
   * markdown: renderizar sintaxe truncada produz lixo visível.
   */
  previa: string | null;
};

export type Alternativa = {
  id: number;
  letra: string;
  texto: string | null;
  imagemUrl: string | null;
};

export type QuestaoDetalhe = {
  id: number;
  ano: number;
  numero: number;
  area: Area;
  idioma: Idioma | null;
  /** null = questão ainda não classificada. */
  materia: Rotulo | null;
  assunto: Rotulo | null;
  /** Nullable: 5% das questões não têm texto-base (ver migration V3). */
  enunciado: string | null;
  comando: string | null;
  gabarito: string | null;
  alternativas: Alternativa[];
};

/**
 * Envelope de página como o Spring Boot 4 serializa — CONFERIDO na API real,
 * não deduzido:
 *
 *     { "content": [...], "page": { size, number, totalElements, totalPages } }
 *
 * Mudou em relação ao Boot 3, que achatava tudo no topo do objeto e ainda
 * trazia `first`/`last`. Quem escrever o tipo de memória erra: os campos
 * chegam como `undefined` e a tela mostra "undefined questões".
 */
type EnvelopeDePagina<T> = {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
};

/**
 * Forma achatada que as telas consomem. Normalizar aqui deixa o formato do
 * Spring confinado a este arquivo — se ele mudar de novo, muda um lugar só.
 */
export type Pagina<T> = {
  itens: T[];
  /** Zero-based, como o Spring Data. */
  pagina: number;
  totalDeItens: number;
  totalDePaginas: number;
};

export function normalizarPagina<T>(envelope: EnvelopeDePagina<T>): Pagina<T> {
  return {
    itens: envelope.content ?? [],
    pagina: envelope.page?.number ?? 0,
    totalDeItens: envelope.page?.totalElements ?? 0,
    totalDePaginas: envelope.page?.totalPages ?? 0,
  };
}

export type FiltroDeQuestoes = {
  area?: Area;
  idioma?: Idioma;
  anoInicial?: number;
  anoFinal?: number;
  /** Slugs da taxonomia (ex.: "matematica", "probabilidade"). */
  materia?: string;
  assunto?: string;
  /** Zero-based, igual ao Spring Data. */
  pagina?: number;
};

export const QUESTOES_POR_PAGINA = 20;

/**
 * Monta a query string da busca.
 *
 * Campo vazio é OMITIDO, não mandado em branco: `?area=` chegaria no Spring
 * como string vazia e a conversão pro enum falharia com 400 — um filtro
 * "limpo" viraria erro de validação em vez de "sem filtro".
 *
 * Exportada para teste: é lógica de tradução que quebra em silêncio (o usuário
 * só veria "nenhuma questão encontrada" sem entender por quê).
 */
export function montarQueryDeBusca(filtro: FiltroDeQuestoes): string {
  const params = new URLSearchParams();

  if (filtro.area) params.set("area", filtro.area);
  if (filtro.idioma) params.set("idioma", filtro.idioma);
  if (filtro.anoInicial !== undefined) params.set("anoInicial", String(filtro.anoInicial));
  if (filtro.anoFinal !== undefined) params.set("anoFinal", String(filtro.anoFinal));
  if (filtro.materia?.trim()) params.set("materia", filtro.materia.trim());
  if (filtro.assunto?.trim()) params.set("assunto", filtro.assunto.trim());
  if (filtro.pagina) params.set("page", String(filtro.pagina));

  params.set("size", String(QUESTOES_POR_PAGINA));
  return params.toString();
}

export async function buscarQuestoes(filtro: FiltroDeQuestoes): Promise<Pagina<QuestaoResumo>> {
  const envelope = await requisitar<EnvelopeDePagina<QuestaoResumo>>(
    `/questoes?${montarQueryDeBusca(filtro)}`,
  );
  return normalizarPagina(envelope);
}

/**
 * Anos com questões disponíveis, do mais recente pro mais antigo.
 *
 * O filtro de ano usa ISTO em vez de um intervalo fixo: o intervalo vem dos
 * dados importados, que espelham o que a enem.dev registra. Prova nova
 * importada aparece sozinha no filtro, sem mexer em código.
 */
export function buscarAnosDisponiveis(): Promise<number[]> {
  return requisitar<number[]>("/questoes/anos");
}

export function buscarQuestaoPorId(id: number): Promise<QuestaoDetalhe> {
  return requisitar<QuestaoDetalhe>(`/questoes/${id}`);
}
