import type { Idioma } from "@/lib/api/questoes";
import type { EscolhaDoPedido, PedidoDeSimulado, TipoDeEscolha } from "@/lib/api/simulados";
import type { AssuntoDaTaxonomia, Taxonomia } from "@/lib/api/taxonomia";

/**
 * Regras da tela de montar prova (Etapa 3a), sem React: o que cada escolha
 * contém, quantas questões há nela, quando o idioma importa e como o formulário
 * vira o pedido da API.
 *
 * Espelham as regras do backend (CriacaoDeSimulado) pra tela responder na hora.
 * Quem DECIDE é o backend: se as duas divergirem, vale o 422 dele.
 */

/** Uma escolha na lista da tela: o que vai pro pedido, mais o nome pra exibir. */
export type EscolhaNaTela = EscolhaDoPedido & { rotulo: string };

// Matérias de língua estrangeira (slugs da V5): só entram numa área inteira se
// o pedido escolher o idioma — a prova nunca mistura as duas versões da questão
const MATERIA_DO_IDIOMA: Record<Idioma, string> = { INGLES: "ingles", ESPANHOL: "espanhol" };
const MATERIAS_ESTRANGEIRAS = new Set(Object.values(MATERIA_DO_IDIOMA));

/** Os assuntos que uma escolha cobre, como o backend os expande. */
export function assuntosDaEscolha(
  taxonomia: Taxonomia,
  escolha: Pick<EscolhaDoPedido, "tipo" | "valor">,
  idioma: Idioma | undefined,
): AssuntoDaTaxonomia[] {
  const materias = taxonomia.flatMap((a) => a.materias.map((m) => ({ area: a.area, ...m })));
  switch (escolha.tipo) {
    case "AREA": {
      const lingua = idioma ? MATERIA_DO_IDIOMA[idioma] : undefined;
      return materias
        .filter((m) => m.area === escolha.valor)
        .filter((m) => !MATERIAS_ESTRANGEIRAS.has(m.slug) || m.slug === lingua)
        .flatMap((m) => m.assuntos);
    }
    case "MATERIA":
      // Matéria escolhida explicitamente (inclusive Inglês) entra inteira
      return materias.find((m) => m.slug === escolha.valor)?.assuntos ?? [];
    case "ASSUNTO":
      return materias.flatMap((m) => m.assuntos).filter((a) => a.slug === escolha.valor);
  }
}

/** Quantas questões disponíveis a escolha tem (nos anos da taxonomia carregada). */
export function questoesDaEscolha(
  taxonomia: Taxonomia,
  escolha: Pick<EscolhaDoPedido, "tipo" | "valor">,
  idioma: Idioma | undefined,
): number {
  return assuntosDaEscolha(taxonomia, escolha, idioma).reduce((soma, a) => soma + a.questoes, 0);
}

/**
 * O idioma só muda alguma coisa quando Linguagens INTEIRA está nas escolhas:
 * matéria e assunto de língua estrangeira já dizem o idioma sozinhos. Fora
 * disso a tela nem mostra o campo, e o pedido não o leva.
 */
export function precisaDeIdioma(escolhas: Pick<EscolhaDoPedido, "tipo" | "valor">[]): boolean {
  return escolhas.some((e) => e.tipo === "AREA" && e.valor === "LINGUAGENS");
}

/**
 * Por que a escolha nova não pode entrar na lista, ou null se pode. Mesma regra
 * do backend: nenhum assunto em duas escolhas ("Probabilidade já está dentro de
 * Matemática"). Avisar ao adicionar poupa o usuário de montar a lista toda e
 * só descobrir no envio.
 */
export function motivoParaRecusar(
  taxonomia: Taxonomia,
  escolhas: EscolhaNaTela[],
  nova: EscolhaNaTela,
  idioma: Idioma | undefined,
): string | null {
  const daNova = new Set(assuntosDaEscolha(taxonomia, nova, idioma).map((a) => a.slug));
  for (const escolha of escolhas) {
    if (escolha.tipo === nova.tipo && escolha.valor === nova.valor) {
      return `${nova.rotulo} já está na prova`;
    }
    const daEscolha = assuntosDaEscolha(taxonomia, escolha, idioma).map((a) => a.slug);
    if (daEscolha.some((slug) => daNova.has(slug))) {
      // A menor está dentro da maior
      return daEscolha.length <= daNova.size
        ? `${escolha.rotulo} já está dentro de ${nova.rotulo}: tire uma das duas`
        : `${nova.rotulo} já está dentro de ${escolha.rotulo}`;
    }
  }
  return null;
}

/**
 * O que impede a prova de sair, conferível sem o banco (os de disponibilidade
 * vêm do 422). Mesmas regras do DistribuidorDeQuestoes: toda escolha leva pelo
 * menos 1 questão, e as quantidades fixas não podem passar do total.
 */
export function problemasDaMontagem(total: number, escolhas: Pick<EscolhaDoPedido, "quantidade">[]): string[] {
  if (escolhas.length === 0 || !Number.isInteger(total)) return [];
  // Mais escolhas que questões já explica tudo; as mensagens de soma repetiriam
  if (escolhas.length > total) {
    return [`${escolhas.length} escolhas não cabem numa prova de ${total} questões: cada uma leva pelo menos 1`];
  }
  const problemas: string[] = [];
  const fixas = escolhas.filter((e) => e.quantidade !== undefined);
  const somaFixas = fixas.reduce((soma, e) => soma + (e.quantidade ?? 0), 0);
  const livres = escolhas.length - fixas.length;
  if (livres === 0 && somaFixas !== total) {
    problemas.push(`As quantidades somam ${somaFixas}, mas a prova tem ${total} questões`);
  } else if (livres > 0 && total - somaFixas < livres) {
    problemas.push(`As quantidades fixas somam ${somaFixas} e não sobra 1 questão pra cada uma das outras escolhas`);
  }
  return problemas;
}

export type FormularioDeMontagem = {
  titulo: string;
  total: number;
  anos: number[];
  idioma?: Idioma;
  itens: EscolhaNaTela[];
  /** Na tela é em minutos; a API recebe segundos. */
  tempoLimiteMin?: number;
};

/** O formulário validado, no formato da API. */
export function montarPedido(formulario: FormularioDeMontagem): PedidoDeSimulado {
  const pedido: PedidoDeSimulado = {
    titulo: formulario.titulo.trim(),
    total: formulario.total,
    anos: [...formulario.anos].sort((a, b) => a - b),
    // O nome de exibição fica na tela; a API só quer tipo, valor e quantidade
    itens: formulario.itens.map(({ tipo, valor, quantidade }) =>
      quantidade === undefined ? { tipo, valor } : { tipo, valor, quantidade },
    ),
  };
  // Idioma escolhido e depois Linguagens removida: não vai, pra não pesar no pedido
  if (formulario.idioma && precisaDeIdioma(formulario.itens)) pedido.idioma = formulario.idioma;
  if (formulario.tempoLimiteMin !== undefined) pedido.tempoLimiteSeg = formulario.tempoLimiteMin * 60;
  return pedido;
}

/** A escolha mais específica que a cascata área → matéria → assunto aponta. */
export function escolhaDaCascata(selecao: {
  area?: string;
  materia?: string;
  assunto?: string;
}): { tipo: TipoDeEscolha; valor: string } | null {
  if (selecao.assunto) return { tipo: "ASSUNTO", valor: selecao.assunto };
  if (selecao.materia) return { tipo: "MATERIA", valor: selecao.materia };
  if (selecao.area) return { tipo: "AREA", valor: selecao.area };
  return null;
}

/**
 * Nome do campo do formulário pra um erro de campo do backend (400 com
 * `campos[]`): `itens[0].quantidade` → `itens.0.quantidade` (notação do
 * react-hook-form) e `tempoLimiteSeg` → `tempoLimiteMin` (a tela usa minutos).
 */
export function campoDoFormulario(campoDaApi: string): string {
  const pontuado = campoDaApi.replace(/\[(\d+)\]/g, ".$1");
  return pontuado === "tempoLimiteSeg" ? "tempoLimiteMin" : pontuado;
}
