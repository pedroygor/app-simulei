import type { FiltroDeQuestoes } from "@/lib/api/questoes";
import { montarUrlDeQuestoes } from "./filtro-da-url";

const LISTAGEM = "/questoes";

/**
 * Lê o id do segmento dinâmico `/questoes/[id]`.
 *
 * Regex em vez de `Number(texto)`: o Number aceita "1e3" (vira 1000), " 12 ",
 * "0x1A" e "12.0" — quatro URLs diferentes apontando pra mesma questão, ou
 * pra uma que o usuário nunca pediu. Id inválido vira 404 sem nem chamar a API.
 */
export function lerIdDaQuestao(texto: string): number | null {
  if (!/^[1-9]\d{0,15}$/.test(texto)) return null;
  const id = Number(texto);
  return Number.isSafeInteger(id) ? id : null;
}

/**
 * Link do cartão pro detalhe, levando junto a listagem de onde o usuário veio.
 *
 * Sem isto o "voltar" do detalhe iria pra `/questoes` e o usuário perderia
 * área, ano e página. Não dá pra confiar no histórico do navegador: numa SPA
 * o document.referrer não muda, e history.length conta páginas de outros sites.
 */
export function montarUrlDeDetalhe(id: number, filtro: FiltroDeQuestoes): string {
  const listagem = montarUrlDeQuestoes(filtro);
  if (listagem === LISTAGEM) return `${LISTAGEM}/${id}`;
  return `${LISTAGEM}/${id}?voltar=${encodeURIComponent(listagem)}`;
}

/**
 * Pra onde o "voltar" do detalhe leva — com proteção contra OPEN REDIRECT.
 *
 * O `?voltar=` vem da URL, e a URL é de quem a montou. Aceitar qualquer
 * destino permitiria `?voltar=https://site-falso.com`: o link é do nosso
 * domínio, parece legítimo, e o "Voltar" entrega o aluno pra outro site. Pior
 * ainda, `//site-falso.com` e `/\site-falso.com` começam com barra mas o
 * navegador trata como outro host.
 *
 * Por isso não se tenta "limpar" o destino: só existe UM válido, a própria
 * listagem com sua query. Qualquer outra coisa cai na listagem sem filtro.
 */
export function destinoDeVolta(valor: string | string[] | undefined): string {
  const texto = Array.isArray(valor) ? valor[0] : valor;
  if (texto !== undefined && /^\/questoes(\?[\s\S]*)?$/.test(texto)) {
    return texto;
  }
  return LISTAGEM;
}
