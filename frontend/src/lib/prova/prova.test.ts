import { describe, expect, it } from "vitest";
import { contarEmBranco, indiceInicial, lerIdDaTentativa, marcadasIniciais } from "./prova";

describe("lerIdDaTentativa", () => {
  it("aceita UUID e normaliza pra minúsculas", () => {
    expect(lerIdDaTentativa("0F8FAD5B-D9CB-469F-A165-70867728950E")).toBe("0f8fad5b-d9cb-469f-a165-70867728950e");
  });

  it("recusa o que não é UUID", () => {
    for (const ruim of ["123", "abc", "0f8fad5b-d9cb-469f-a165-70867728950", "../simulados", ""]) {
      expect(lerIdDaTentativa(ruim)).toBeNull();
    }
  });
});

describe("indiceInicial", () => {
  it("abre na primeira questão em branco", () => {
    expect(indiceInicial([{ marcada: 1 }, { marcada: 2 }, { marcada: null }, { marcada: null }])).toBe(2);
  });

  it("prova nova ou toda respondida abre na primeira", () => {
    expect(indiceInicial([{ marcada: null }, { marcada: null }])).toBe(0);
    expect(indiceInicial([{ marcada: 1 }, { marcada: 2 }])).toBe(0);
  });
});

describe("contarEmBranco", () => {
  it("conta o que está desmarcado ou nunca foi marcado", () => {
    const questoes = [{ id: 1 }, { id: 2 }, { id: 3 }];
    expect(contarEmBranco(questoes, new Map([[1, 10], [2, null]]))).toBe(2);
  });
});

describe("marcadasIniciais", () => {
  it("leva o que o servidor tinha pra fila, incluindo as em branco", () => {
    expect(marcadasIniciais([{ id: 1, marcada: 10 }, { id: 2, marcada: null }])).toEqual(
      new Map([
        [1, 10],
        [2, null],
      ]),
    );
  });
});
