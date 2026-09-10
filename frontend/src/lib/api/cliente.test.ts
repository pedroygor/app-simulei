import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const URL_DA_API = "http://localhost:8080";

/**
 * O cliente guarda estado de módulo (token em memória, renovação em
 * andamento, observador de sessão). Recarregar os módulos a cada teste
 * garante que um teste não veja o estado deixado pelo anterior.
 */
async function carregarCliente() {
  vi.resetModules();
  const cliente = await import("./cliente");
  const token = await import("./token");
  const erros = await import("./erros");
  return { ...cliente, ...token, ...erros };
}

function resposta(status: number, corpo?: unknown, cabecalhos: Record<string, string> = {}) {
  return new Response(corpo === undefined ? null : JSON.stringify(corpo), {
    status,
    headers: { "content-type": "application/json", ...cabecalhos },
  });
}

let fetchFalso: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchFalso = vi.fn();
  vi.stubGlobal("fetch", fetchFalso);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("requisitar", () => {
  it("anexa o token e devolve o corpo já desserializado", async () => {
    const { requisitar, guardarAccessToken } = await carregarCliente();
    guardarAccessToken("token-abc");
    fetchFalso.mockResolvedValueOnce(resposta(200, { nome: "Ana" }));

    const dados = await requisitar<{ nome: string }>("/usuarios/me");

    expect(dados).toEqual({ nome: "Ana" });
    const [url, opcoes] = fetchFalso.mock.calls[0];
    expect(url).toBe(`${URL_DA_API}/usuarios/me`);
    expect((opcoes.headers as Record<string, string>).Authorization).toBe("Bearer token-abc");
    // Sem credentials o cookie httpOnly do refresh não viaja cross-origin
    expect(opcoes.credentials).toBe("include");
  });

  it("transforma erro do servidor em exceção com status e campos", async () => {
    const { requisitar, ErroDaApi } = await carregarCliente();
    fetchFalso.mockResolvedValueOnce(
      resposta(400, {
        status: 400,
        mensagem: "Há campos inválidos na requisição",
        campos: [{ campo: "email", mensagem: "E-mail inválido" }],
      }),
    );

    // Erro é exceção, nunca valor de retorno: quem não tratar quebra alto.
    // Uma chamada só — o mock foi armado com mockResolvedValueOnce.
    const erro = await requisitar("/usuarios", { method: "POST", body: {} }).catch((e) => e);

    expect(erro).toBeInstanceOf(ErroDaApi);
    const api = erro as InstanceType<typeof ErroDaApi>;
    expect(api.status).toBe(400);
    expect(api.temErrosDeCampo).toBe(true);
    expect(api.campos[0].campo).toBe("email");
  });

  it("transforma falha de rede em ErroDeRede", async () => {
    const { requisitar, ErroDeRede } = await carregarCliente();
    fetchFalso.mockRejectedValueOnce(new TypeError("Failed to fetch"));

    await expect(requisitar("/questoes")).rejects.toBeInstanceOf(ErroDeRede);
  });
});

describe("renovação automática em 401", () => {
  it("renova e repete a requisição uma vez", async () => {
    const { requisitar, guardarAccessToken, lerAccessToken } = await carregarCliente();
    guardarAccessToken("token-velho");
    fetchFalso
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "expirado" }))
      .mockResolvedValueOnce(resposta(200, { accessToken: "token-novo" }))
      .mockResolvedValueOnce(resposta(200, { id: 1 }));

    const dados = await requisitar<{ id: number }>("/questoes");

    expect(dados).toEqual({ id: 1 });
    expect(lerAccessToken()).toBe("token-novo");
    expect(fetchFalso.mock.calls.map((c) => c[0])).toEqual([
      `${URL_DA_API}/questoes`,
      `${URL_DA_API}/auth/refresh`,
      `${URL_DA_API}/questoes`,
    ]);
  });

  it("não tenta renovar de novo se a repetição também der 401", async () => {
    const { requisitar, guardarAccessToken, ErroDaApi } = await carregarCliente();
    guardarAccessToken("token-velho");
    fetchFalso
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "expirado" }))
      .mockResolvedValueOnce(resposta(200, { accessToken: "token-novo" }))
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "ainda inválido" }));

    // Sem o freio do jaRenovou isto viraria laço infinito de renovação
    await expect(requisitar("/questoes")).rejects.toBeInstanceOf(ErroDaApi);
    expect(fetchFalso.mock.calls.filter((c) => String(c[0]).includes("/auth/refresh"))).toHaveLength(1);
  });

  it("não tenta renovar quando o próprio login devolve 401", async () => {
    const { requisitar, ErroDaApi } = await carregarCliente();
    fetchFalso.mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "senha errada" }));

    await expect(
      requisitar("/auth/login", { method: "POST", body: { email: "a@b.c", senha: "x" } }),
    ).rejects.toBeInstanceOf(ErroDaApi);
    // Renovar aqui seria absurdo: quem errou a senha não tem sessão pra renovar
    expect(fetchFalso).toHaveBeenCalledTimes(1);
  });

  it("renova UMA vez só quando várias requisições expiram juntas", async () => {
    const { requisitar, guardarAccessToken } = await carregarCliente();
    guardarAccessToken("token-velho");

    // Este é o cenário que motivou a deduplicação: o backend ROTACIONA o
    // refresh token a cada uso. Se três consultas renovassem em paralelo, a
    // segunda usaria um token já invalidado pela primeira e derrubaria a
    // sessão de um usuário que não fez nada de errado.
    fetchFalso.mockImplementation((url: string) => {
      if (String(url).includes("/auth/refresh")) {
        return Promise.resolve(resposta(200, { accessToken: "token-novo" }));
      }
      const chamadasDoRecurso = fetchFalso.mock.calls.filter(
        (c) => !String(c[0]).includes("/auth/refresh"),
      ).length;
      // As três primeiras (uma por consulta) dão 401; as repetições passam
      return Promise.resolve(
        chamadasDoRecurso <= 3 ? resposta(401, { status: 401, mensagem: "expirado" }) : resposta(200, { ok: true }),
      );
    });

    await Promise.all([requisitar("/questoes"), requisitar("/usuarios/me"), requisitar("/questoes/1")]);

    const renovacoes = fetchFalso.mock.calls.filter((c) => String(c[0]).includes("/auth/refresh"));
    expect(renovacoes).toHaveLength(1);
  });
});

describe("perda de sessão", () => {
  it("avisa o observador quando a renovação é recusada", async () => {
    const { requisitar, guardarAccessToken, observarPerdaDeSessao, lerAccessToken, ErroDaApi } =
      await carregarCliente();
    guardarAccessToken("token-velho");
    const avisado = vi.fn();
    observarPerdaDeSessao(avisado);

    fetchFalso
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "expirado" }))
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "refresh recusado" }));

    // Era o bug: o token era limpo mas a interface continuava achando que
    // havia sessão, e o usuário travava numa tela que não carregava
    await expect(requisitar("/questoes")).rejects.toBeInstanceOf(ErroDaApi);
    expect(avisado).toHaveBeenCalledOnce();
    expect(lerAccessToken()).toBeNull();
  });

  it("avisa também quando a renovação falha por rede", async () => {
    const { requisitar, guardarAccessToken, observarPerdaDeSessao } = await carregarCliente();
    guardarAccessToken("token-velho");
    const avisado = vi.fn();
    observarPerdaDeSessao(avisado);

    fetchFalso
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "expirado" }))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"));

    await expect(requisitar("/questoes")).rejects.toBeTruthy();
    expect(avisado).toHaveBeenCalledOnce();
  });

  it("para de avisar depois que o observador é removido", async () => {
    const { requisitar, guardarAccessToken, observarPerdaDeSessao } = await carregarCliente();
    guardarAccessToken("token-velho");
    const avisado = vi.fn();
    observarPerdaDeSessao(avisado);
    observarPerdaDeSessao(null);

    fetchFalso
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "expirado" }))
      .mockResolvedValueOnce(resposta(401, { status: 401, mensagem: "refresh recusado" }));

    await expect(requisitar("/questoes")).rejects.toBeTruthy();
    expect(avisado).not.toHaveBeenCalled();
  });
});
