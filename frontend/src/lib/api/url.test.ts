import { describe, expect, it } from "vitest";
import { resolverUrlDeImagem } from "./url";

describe("resolverUrlDeImagem", () => {
  it("prefixa a base da API em caminho relativo", () => {
    // Sem isto o navegador pediria a imagem pro Next (:3000) e levaria 404 —
    // a API é quem serve /imagens
    expect(resolverUrlDeImagem("/imagens/3b/0e/hash.png")).toBe(
      "http://localhost:8080/imagens/3b/0e/hash.png",
    );
  });

  it("prefixa mesmo sem a barra inicial", () => {
    expect(resolverUrlDeImagem("imagens/ab/cd/hash.png")).toBe(
      "http://localhost:8080/imagens/ab/cd/hash.png",
    );
  });

  it("deixa URL absoluta intacta", () => {
    // As imagens recusadas pelo allowlist mantêm a URL da fonte de propósito
    expect(resolverUrlDeImagem("https://enem.dev/broken-image.svg")).toBe(
      "https://enem.dev/broken-image.svg",
    );
    expect(resolverUrlDeImagem("http://exemplo.com/a.png")).toBe("http://exemplo.com/a.png");
  });

  it("reconhece o esquema sem depender de caixa", () => {
    expect(resolverUrlDeImagem("HTTPS://enem.dev/a.png")).toBe("HTTPS://enem.dev/a.png");
  });
});
