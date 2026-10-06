import { describe, expect, it } from "vitest";
import { estadoNaRevisao, situacaoNaRevisao } from "./correcao";

const alternativas = ["A", "B", "C", "D", "E"].map((letra, i) => ({ id: 10 + i, letra }));
const [a, b] = alternativas;

describe("estadoNaRevisao", () => {
  it("marcou o gabarito: acertou; as outras ficam neutras", () => {
    const questao = { marcada: a.id, gabarito: "A" };
    expect(estadoNaRevisao(a, questao)).toBe("acertou");
    expect(estadoNaRevisao(b, questao)).toBe("neutra");
  });

  it("marcou errado: a dele é incorreta e o gabarito aparece como correta", () => {
    const questao = { marcada: b.id, gabarito: "A" };
    expect(estadoNaRevisao(b, questao)).toBe("incorreta");
    expect(estadoNaRevisao(a, questao)).toBe("correta");
  });

  it("em branco: só o gabarito se destaca", () => {
    const questao = { marcada: null, gabarito: "A" };
    expect(alternativas.map((alt) => estadoNaRevisao(alt, questao))).toEqual([
      "correta", "neutra", "neutra", "neutra", "neutra",
    ]);
  });
});

describe("situacaoNaRevisao", () => {
  it("compara a LETRA da marcada com o gabarito", () => {
    expect(situacaoNaRevisao({ marcada: a.id, gabarito: "A", alternativas })).toBe("acertou");
    expect(situacaoNaRevisao({ marcada: b.id, gabarito: "A", alternativas })).toBe("errou");
    expect(situacaoNaRevisao({ marcada: null, gabarito: "A", alternativas })).toBe("emBranco");
  });
});
