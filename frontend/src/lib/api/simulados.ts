import { requisitar } from "./cliente";
import type { Area, Idioma, Rotulo } from "./questoes";

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
