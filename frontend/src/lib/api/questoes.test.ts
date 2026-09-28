import { describe, expect, it } from "vitest";
import { montarQueryDeBusca, normalizarPagina, QUESTOES_POR_PAGINA } from "./questoes";

const parse = (qs: string) => Object.fromEntries(new URLSearchParams(qs));

describe("montarQueryDeBusca", () => {
  it("sempre fixa o tamanho da página", () => {
    expect(parse(montarQueryDeBusca({})).size).toBe(String(QUESTOES_POR_PAGINA));
  });

  it("omite filtro não preenchido em vez de mandar vazio", () => {
    // `?area=` chegaria no Spring como string vazia e a conversão pro enum
    // falharia com 400: limpar o filtro viraria erro de validação
    const params = parse(montarQueryDeBusca({}));

    expect(params).not.toHaveProperty("area");
    expect(params).not.toHaveProperty("idioma");
    expect(params).not.toHaveProperty("materia");
    expect(params).not.toHaveProperty("anoInicial");
  });

  it("manda os filtros preenchidos", () => {
    const params = parse(
      montarQueryDeBusca({
        area: "MATEMATICA",
        idioma: "INGLES",
        anoInicial: 2015,
        anoFinal: 2023,
      }),
    );

    expect(params.area).toBe("MATEMATICA");
    expect(params.idioma).toBe("INGLES");
    expect(params.anoInicial).toBe("2015");
    expect(params.anoFinal).toBe("2023");
  });

  it("descarta matéria só com espaço e apara a preenchida", () => {
    expect(parse(montarQueryDeBusca({ materia: "   " }))).not.toHaveProperty("materia");
    expect(parse(montarQueryDeBusca({ materia: "  álgebra " })).materia).toBe("álgebra");
  });

  it("omite a página zero, que é o padrão do Spring Data", () => {
    expect(parse(montarQueryDeBusca({ pagina: 0 }))).not.toHaveProperty("page");
    expect(parse(montarQueryDeBusca({ pagina: 3 })).page).toBe("3");
  });

  it("codifica caractere especial da matéria", () => {
    // "ciências & afins" sem escape quebraria a query string
    const qs = montarQueryDeBusca({ materia: "ciências & afins" });

    expect(qs).not.toContain(" & afins");
    expect(parse(qs).materia).toBe("ciências & afins");
  });
});

describe("normalizarPagina — o envelope do Spring Boot 4", () => {
  /**
   * Corpo REAL de GET /questoes?size=20, capturado da API rodando. Não é
   * inventado: o Boot 4 aninha os metadados em `page`, enquanto o Boot 3
   * achatava tudo no topo e ainda mandava `first`/`last`. Escrevi o tipo de
   * memória primeiro e a tela veio com "undefined questões" — este teste
   * existe pra essa quebra não voltar calada.
   */
  const ENVELOPE_REAL = {
    content: [{ id: 176 }, { id: 177 }],
    page: { size: 20, number: 0, totalElements: 182, totalPages: 10 },
  };

  it("acha os metadados dentro de page, não no topo", () => {
    expect(normalizarPagina(ENVELOPE_REAL)).toEqual({
      itens: [{ id: 176 }, { id: 177 }],
      pagina: 0,
      totalDeItens: 182,
      totalDePaginas: 10,
    });
  });

  it("preserva a página corrente em página que não é a primeira", () => {
    const pagina = normalizarPagina({ ...ENVELOPE_REAL, page: { ...ENVELOPE_REAL.page, number: 3 } });

    expect(pagina.pagina).toBe(3);
  });

  it("não quebra se o envelope vier sem metadados", () => {
    // Um 200 malformado não pode virar NaN espalhado pela paginação
    const pagina = normalizarPagina({ content: [] } as unknown as Parameters<typeof normalizarPagina>[0]);

    expect(pagina).toEqual({ itens: [], pagina: 0, totalDeItens: 0, totalDePaginas: 0 });
  });
});
