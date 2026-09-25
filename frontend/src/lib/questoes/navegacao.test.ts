import { describe, expect, it } from "vitest";
import { destinoDeVolta, lerIdDaQuestao, montarUrlDeDetalhe } from "./navegacao";

describe("lerIdDaQuestao", () => {
  it("aceita inteiro positivo", () => {
    expect(lerIdDaQuestao("176")).toBe(176);
    expect(lerIdDaQuestao("1")).toBe(1);
  });

  it("recusa o que o Number() aceitaria por engano", () => {
    // Todas essas o Number() converteria num número "válido"
    for (const texto of ["1e3", " 12", "12 ", "0x1A", "12.0", "+5", "012", ""]) {
      expect(lerIdDaQuestao(texto), texto).toBeNull();
    }
  });

  it("recusa zero, negativo e texto", () => {
    expect(lerIdDaQuestao("0")).toBeNull();
    expect(lerIdDaQuestao("-3")).toBeNull();
    expect(lerIdDaQuestao("abc")).toBeNull();
  });

  it("recusa número além do inteiro seguro do JavaScript", () => {
    // Acima de 2^53 o Number arredonda: dois ids diferentes virariam o mesmo
    expect(lerIdDaQuestao("9999999999999999")).toBeNull();
  });
});

describe("destinoDeVolta — proteção contra open redirect", () => {
  it("aceita a listagem, com ou sem filtros", () => {
    expect(destinoDeVolta("/questoes")).toBe("/questoes");
    expect(destinoDeVolta("/questoes?area=MATEMATICA&pagina=2")).toBe("/questoes?area=MATEMATICA&pagina=2");
  });

  it("recusa outro host, em todas as formas que o navegador entende como host", () => {
    for (const ataque of [
      "https://site-falso.com",
      "//site-falso.com",
      "/\\site-falso.com",
      "javascript:alert(1)",
      "/questoes@site-falso.com",
    ]) {
      expect(destinoDeVolta(ataque), ataque).toBe("/questoes");
    }
  });

  it("recusa outras rotas do próprio app", () => {
    // Só a listagem é destino de volta — nada de mandar pro /entrar ou pra
    // outra questão
    expect(destinoDeVolta("/entrar")).toBe("/questoes");
    expect(destinoDeVolta("/questoes/176")).toBe("/questoes");
    expect(destinoDeVolta("/questoesX")).toBe("/questoes");
  });

  it("cai na listagem quando não veio nada", () => {
    expect(destinoDeVolta(undefined)).toBe("/questoes");
    expect(destinoDeVolta(["/questoes?area=MATEMATICA", "/entrar"])).toBe("/questoes?area=MATEMATICA");
  });
});

describe("montarUrlDeDetalhe", () => {
  it("não polui a URL quando a listagem não tem filtro", () => {
    expect(montarUrlDeDetalhe(176, {})).toBe("/questoes/176");
  });

  it("leva a listagem filtrada e o detalhe devolve pra ela intacta", () => {
    // Ida e volta: o que o cartão manda é exatamente o que o voltar aceita
    const filtro = { area: "MATEMATICA" as const, anoInicial: 2022, anoFinal: 2022, pagina: 3 };
    const url = new URL(montarUrlDeDetalhe(176, filtro), "http://x");

    expect(url.pathname).toBe("/questoes/176");
    expect(destinoDeVolta(url.searchParams.get("voltar") ?? undefined)).toBe(
      "/questoes?area=MATEMATICA&anoInicial=2022&anoFinal=2022&pagina=4",
    );
  });
});
