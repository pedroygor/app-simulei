import { requisitar } from "./cliente";
import { normalizarPagina, type Area, type EnvelopeDePagina, type Idioma, type Pagina, type Rotulo } from "./questoes";
import type { Placar } from "./resultados";

/** Espelham os DTOs de SimuladoController no backend. */

export const TIPOS_DE_ESCOLHA = ["AREA", "MATERIA", "ASSUNTO"] as const;
export type TipoDeEscolha = (typeof TIPOS_DE_ESCOLHA)[number];

export type EscolhaDoPedido = {
  tipo: TipoDeEscolha;
  /** Enum da área (MATEMATICA) ou slug da matéria/assunto. */
  valor: string;
  /** Ausente = o sistema distribui. */
  quantidade?: number;
};

export type PedidoDeSimulado = {
  titulo: string;
  total: number;
  /** Vazio = todos os anos. */
  anos: number[];
  /** Ausente = sem língua estrangeira ao escolher Linguagens inteira. */
  idioma?: Idioma;
  itens: EscolhaDoPedido[];
  tempoLimiteSeg?: number;
};

export type SimuladoCriado = {
  id: string;
  titulo: string;
  total: number;
  tempoLimiteSeg: number | null;
  criadoEm: string;
  /** Quanto saiu de cada escolha, na ordem do pedido. */
  composicao: { rotulo: string; quantidade: number }[];
};

export type Simulado = {
  id: string;
  titulo: string;
  total: number;
  tempoLimiteSeg: number | null;
  criadoEm: string;
  composicao: { area: Area; materia: Rotulo; assunto: Rotulo; questoes: number }[];
};

export function criarSimulado(pedido: PedidoDeSimulado): Promise<SimuladoCriado> {
  return requisitar<SimuladoCriado>("/simulados", { method: "POST", body: pedido });
}

/** Como a prova aparece em "minhas provas" (espelha SituacaoDaProva do backend). */
export type SituacaoDaProva = "NAO_INICIADA" | "EM_ANDAMENTO" | "ENTREGUE";

export type ProvaDoHistorico = {
  simuladoId: string;
  titulo: string;
  criadoEm: string;
  situacao: SituacaoDaProva;
  /** null = não iniciada. */
  tentativaId: string | null;
  iniciadoEm: string | null;
  finalizadoEm: string | null;
  questoes: number;
  respondidas: number;
  /** Só da prova entregue: com ela aberta, acertos vazariam o gabarito. */
  placar: Placar | null;
};

export const PROVAS_POR_PAGINA = 20;

/** GET /simulados: as provas do usuário, da mais nova pra mais velha. */
export async function buscarMinhasProvas(pagina: number): Promise<Pagina<ProvaDoHistorico>> {
  const params = new URLSearchParams({ page: String(pagina), size: String(PROVAS_POR_PAGINA) });
  return normalizarPagina(await requisitar<EnvelopeDePagina<ProvaDoHistorico>>(`/simulados?${params}`));
}
