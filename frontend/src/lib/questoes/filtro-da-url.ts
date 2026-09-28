import { AREAS, IDIOMAS, type Area, type FiltroDeQuestoes, type Idioma } from "@/lib/api/questoes";

/**
 * Limites de SANIDADE pra ler a URL — espelham o Bean Validation do backend.
 *
 * NÃO são o intervalo oferecido ao usuário: esse vem de GET /questoes/anos,
 * ou seja, dos dados. Aqui só se descarta o absurdo (`?anoInicial=1500`)
 * antes de gastar uma ida ao servidor.
 */
export const ANO_MINIMO = 2009;
export const ANO_MAXIMO = 2030;

/** Formato que o Next 16 entrega depois do `await searchParams`. */
export type ParametrosDaUrl = Record<string, string | string[] | undefined>;

/** `?area=X&area=Y` chega como array; para filtro único, o primeiro vale. */
function primeiro(valor: string | string[] | undefined): string | undefined {
  return Array.isArray(valor) ? valor[0] : valor;
}

function lerOpcao<T extends string>(
  valor: string | string[] | undefined,
  permitidos: readonly T[],
): T | undefined {
  const texto = primeiro(valor);
  return permitidos.includes(texto as T) ? (texto as T) : undefined;
}

function lerAno(valor: string | string[] | undefined): number | undefined {
  const texto = primeiro(valor);
  if (!texto) return undefined;
  const numero = Number(texto);
  if (!Number.isInteger(numero) || numero < ANO_MINIMO || numero > ANO_MAXIMO) {
    return undefined;
  }
  return numero;
}

/**
 * Traduz a query string da URL em filtro, DESCARTANDO o que for inválido.
 *
 * A URL é editável por qualquer um — `?area=BANANA` ou `?page=-5` chegam aqui
 * como entrada de usuário. Descartar em silêncio é deliberado: a alternativa
 * seria erro de validação numa tela que o usuário só queria abrir. O backend
 * valida de novo de qualquer jeito (BuscarQuestoesFiltro tem Bean Validation);
 * isto evita a ida até lá e uma mensagem de erro sem sentido pro aluno.
 */
export function lerFiltroDaUrl(params: ParametrosDaUrl): FiltroDeQuestoes {
  const anoInicial = lerAno(params.anoInicial);
  const anoFinal = lerAno(params.anoFinal);
  const materia = primeiro(params.materia)?.trim();

  const pagina = Number(primeiro(params.pagina));

  return {
    area: lerOpcao<Area>(params.area, AREAS),
    idioma: lerOpcao<Idioma>(params.idioma, IDIOMAS),
    anoInicial,
    anoFinal,
    materia: materia || undefined,
    // Na URL a página é 1-based (?pagina=2 é a segunda, como o usuário lê),
    // mas o Spring Data é 0-based. A conversão mora AQUI, num lugar só.
    pagina: Number.isInteger(pagina) && pagina > 1 ? pagina - 1 : 0,
  };
}

/**
 * Caminho de volta: filtro → URL. É o inverso de lerFiltroDaUrl e precisa
 * continuar sendo — se os dois discordarem, o usuário clica num filtro e a
 * tela volta com outro estado.
 */
export function montarUrlDeQuestoes(filtro: FiltroDeQuestoes): string {
  const params = new URLSearchParams();

  if (filtro.area) params.set("area", filtro.area);
  if (filtro.idioma) params.set("idioma", filtro.idioma);
  if (filtro.anoInicial !== undefined) params.set("anoInicial", String(filtro.anoInicial));
  if (filtro.anoFinal !== undefined) params.set("anoFinal", String(filtro.anoFinal));
  if (filtro.materia?.trim()) params.set("materia", filtro.materia.trim());
  if (filtro.pagina) params.set("pagina", String(filtro.pagina + 1));

  const query = params.toString();
  return query ? `/questoes?${query}` : "/questoes";
}

/** Rótulos de exibição. Enum do backend não é texto de interface. */
export const NOME_DA_AREA: Record<Area, string> = {
  LINGUAGENS: "Linguagens",
  CIENCIAS_HUMANAS: "Ciências Humanas",
  CIENCIAS_NATUREZA: "Ciências da Natureza",
  MATEMATICA: "Matemática",
};

export const NOME_DO_IDIOMA: Record<Idioma, string> = {
  INGLES: "Inglês",
  ESPANHOL: "Espanhol",
};

/**
 * Anos a oferecer no filtro: os disponíveis, MAIS o que já está selecionado.
 *
 * O Select do Radix só exibe o valor se existir um item com ele. Sem incluir o
 * selecionado, dois casos mostrariam o campo em branco com filtro ativo:
 * enquanto GET /questoes/anos não respondeu, e quando a URL traz um ano sem
 * questões (`?anoInicial=2025`, digitado ou de link antigo). Em branco, o
 * usuário vê lista vazia sem saber que há filtro ligado.
 */
export function montarOpcoesDeAno(disponiveis: number[] | undefined, selecionado: number | undefined): number[] {
  const anos = new Set(disponiveis ?? []);
  if (selecionado !== undefined) anos.add(selecionado);
  return [...anos].sort((a, b) => b - a);
}
