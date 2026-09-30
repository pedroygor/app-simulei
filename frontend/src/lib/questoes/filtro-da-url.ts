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

/**
 * Mesma regra do backend (BuscarQuestoesFiltro.SLUG + @Size(max = 60)).
 * Slug fora do formato é descartado aqui pra não virar 400 na tela; slug no
 * formato mas inexistente passa, e o backend devolve busca vazia.
 */
const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const TAMANHO_MAXIMO_DO_SLUG = 60;

function lerSlug(valor: string | string[] | undefined): string | undefined {
  const texto = primeiro(valor)?.trim();
  if (!texto || texto.length > TAMANHO_MAXIMO_DO_SLUG || !SLUG.test(texto)) return undefined;
  return texto;
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
  const pagina = Number(primeiro(params.pagina));

  return {
    area: lerOpcao<Area>(params.area, AREAS),
    idioma: lerOpcao<Idioma>(params.idioma, IDIOMAS),
    anoInicial,
    anoFinal,
    materia: lerSlug(params.materia),
    assunto: lerSlug(params.assunto),
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
  if (filtro.assunto?.trim()) params.set("assunto", filtro.assunto.trim());
  if (filtro.pagina) params.set("pagina", String(filtro.pagina + 1));

  const query = params.toString();
  return query ? `/questoes?${query}` : "/questoes";
}

/**
 * Aplica uma mudança de filtro mantendo a hierarquia área → matéria → assunto
 * coerente.
 *
 * Trocar a área limpa matéria e assunto; trocar a matéria limpa o assunto. Sem
 * isto a URL ficaria com `?area=MATEMATICA&assunto=genetica` — combinação que
 * nunca tem questão — e o usuário veria lista vazia sem entender por quê.
 *
 * A exceção é a mudança que JÁ traz o nível de baixo: escolher uma matéria
 * manda junto a área dela (`{ area, materia }`), e aí a matéria não pode ser
 * apagada pela troca de área que ela mesma causou.
 *
 * Toda mudança volta pra página 1: quem está na página 7 de Matemática e troca
 * pra Linguagens cairia numa página 7 que provavelmente não existe.
 */
export function mudarFiltro(
  atual: FiltroDeQuestoes,
  mudanca: Partial<FiltroDeQuestoes>,
): FiltroDeQuestoes {
  const novo: FiltroDeQuestoes = { ...atual, ...mudanca, pagina: 0 };

  if ("area" in mudanca && mudanca.area !== atual.area && !("materia" in mudanca)) {
    novo.materia = undefined;
    novo.assunto = undefined;
  }
  if ("materia" in mudanca && mudanca.materia !== atual.materia && !("assunto" in mudanca)) {
    novo.assunto = undefined;
  }
  return novo;
}

/** Algum filtro ligado? Decide se o "Limpar filtros" aparece. */
export function temFiltroAtivo(filtro: FiltroDeQuestoes): boolean {
  return Boolean(
    filtro.area ||
      filtro.idioma ||
      filtro.materia ||
      filtro.assunto ||
      filtro.anoInicial !== undefined ||
      filtro.anoFinal !== undefined,
  );
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

export type OpcaoDeFiltro = { valor: string; rotulo: string };

/**
 * Mesma ideia de montarOpcoesDeAno, pra matéria e assunto: garante que o valor
 * selecionado tenha um item na lista, senão o Select do Radix fica em branco
 * com o filtro ligado. Acontece enquanto a taxonomia não chegou e com slug que
 * não existe (`?assunto=renomeado` de link antigo). Sem o nome, mostra o slug.
 */
export function comOpcaoSelecionada(opcoes: OpcaoDeFiltro[], selecionado: string | undefined): OpcaoDeFiltro[] {
  if (selecionado === undefined || opcoes.some((o) => o.valor === selecionado)) return opcoes;
  return [...opcoes, { valor: selecionado, rotulo: selecionado }];
}
