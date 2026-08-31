import { ErroDaApi, ErroDeRede, type CorpoDeErro } from "./erros";
import { guardarAccessToken, lerAccessToken, limparAccessToken } from "./token";

const URL_DA_API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/** Rotas que NÃO devem disparar refresh automático — senão vira laço. */
const ROTAS_DE_AUTENTICACAO = ["/auth/login", "/auth/refresh"];

type OpcoesDeRequisicao = Omit<RequestInit, "body"> & {
  body?: unknown;
  /** Uso interno: impede retentar depois de já ter renovado uma vez. */
  jaRenovou?: boolean;
};

/**
 * Uma renovação por vez.
 *
 * Numa tela com três consultas em paralelo, o token expira para as três ao
 * mesmo tempo e cada uma tentaria renovar. Como o backend ROTACIONA o refresh
 * token a cada uso, a segunda chamada usaria um token já invalidado pela
 * primeira e derrubaria a sessão de um usuário que não fez nada de errado.
 * Guardando a promessa em andamento, as três esperam a mesma renovação.
 */
let renovacaoEmAndamento: Promise<boolean> | null = null;

async function renovarSessao(): Promise<boolean> {
  renovacaoEmAndamento ??= (async () => {
    try {
      const resposta = await fetch(`${URL_DA_API}/auth/refresh`, {
        method: "POST",
        // Sem isto o cookie httpOnly do refresh não é enviado numa
        // requisição cross-origin — e é ele a credencial aqui
        credentials: "include",
      });
      if (!resposta.ok) {
        limparAccessToken();
        return false;
      }
      const { accessToken } = (await resposta.json()) as { accessToken: string };
      guardarAccessToken(accessToken);
      return true;
    } catch {
      limparAccessToken();
      return false;
    } finally {
      renovacaoEmAndamento = null;
    }
  })();
  return renovacaoEmAndamento;
}

async function lerCorpoDeErro(resposta: Response): Promise<CorpoDeErro | null> {
  try {
    return (await resposta.json()) as CorpoDeErro;
  } catch {
    // Nem toda resposta de erro tem corpo JSON (um 502 de proxy, por exemplo)
    return null;
  }
}

/**
 * Faz a requisição, anexa o token e trata 401 renovando a sessão uma vez.
 *
 * Devolve os dados já desserializados. Erro nunca vira valor de retorno:
 * sai como ErroDaApi (o servidor respondeu com falha) ou ErroDeRede (a
 * requisição não chegou).
 */
export async function requisitar<T>(caminho: string, opcoes: OpcoesDeRequisicao = {}): Promise<T> {
  const { body, jaRenovou, headers, ...resto } = opcoes;
  const token = lerAccessToken();

  let resposta: Response;
  try {
    resposta = await fetch(`${URL_DA_API}${caminho}`, {
      ...resto,
      credentials: "include",
      headers: {
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (causa) {
    throw new ErroDeRede(causa);
  }

  const ehRotaDeAutenticacao = ROTAS_DE_AUTENTICACAO.some((rota) => caminho.startsWith(rota));
  if (resposta.status === 401 && !jaRenovou && !ehRotaDeAutenticacao) {
    const renovou = await renovarSessao();
    if (renovou) {
      return requisitar<T>(caminho, { ...opcoes, jaRenovou: true });
    }
  }

  if (!resposta.ok) {
    const corpo = await lerCorpoDeErro(resposta);
    throw new ErroDaApi(
      resposta.status,
      corpo?.mensagem ?? "Não foi possível completar a operação.",
      corpo?.campos ?? [],
    );
  }

  // 204 e 201 sem corpo não têm JSON pra ler
  if (resposta.status === 204 || resposta.headers.get("content-length") === "0") {
    return undefined as T;
  }
  return (await resposta.json()) as T;
}
