import { describe, expect, it } from "vitest";
import type { Aproveitamento } from "@/lib/api/desempenho";
import { agruparPorArea, faltamRespostas, placarCurto, separarPorAmostra } from "./desempenho";

const ap = (respondidas: number): Aproveitamento => ({
  respondidas,
  acertos: 0,
  emBranco: 0,
  percentual: respondidas === 0 ? null : 0,
  amostraSuficiente: respondidas >= 5,
  pertoDoChute: false,
});
const rotulo = (slug: string) => ({ slug, nome: slug });

describe("agruparPorArea", () => {
  it("põe cada assunto na sua área, na ordem que veio", () => {
    const grupos = agruparPorArea({
      porArea: [
        { area: "CIENCIAS_NATUREZA", nome: "Natureza", aproveitamento: ap(3) },
        { area: "MATEMATICA", nome: "Matemática", aproveitamento: ap(7) },
      ],
      porAssunto: [
        { area: "MATEMATICA", materia: rotulo("matematica"), assunto: rotulo("estatistica"), aproveitamento: ap(4) },
        { area: "CIENCIAS_NATUREZA", materia: rotulo("biologia"), assunto: rotulo("ecologia"), aproveitamento: ap(3) },
        { area: "MATEMATICA", materia: rotulo("matematica"), assunto: rotulo("probabilidade"), aproveitamento: ap(3) },
      ],
    });

    expect(grupos.map((g) => [g.area, g.assuntos.map((a) => a.assunto.slug)])).toEqual([
      ["CIENCIAS_NATUREZA", ["ecologia"]],
      ["MATEMATICA", ["estatistica", "probabilidade"]],
    ]);
  });

  it("área só com questões sem assunto continua aparecendo", () => {
    const grupos = agruparPorArea({
      porArea: [{ area: "MATEMATICA", nome: "Matemática", aproveitamento: ap(2) }],
      porAssunto: [],
    });
    expect(grupos).toHaveLength(1);
    expect(grupos[0].assuntos).toEqual([]);
  });
});

describe("faltamRespostas", () => {
  it("conta o que falta e nunca fica negativo", () => {
    expect(faltamRespostas(ap(2), 5)).toBe(3);
    expect(faltamRespostas(ap(5), 5)).toBe(0);
    expect(faltamRespostas(ap(9), 5)).toBe(0);
  });
});

describe("separarPorAmostra", () => {
  it("separa pela amostra mínima, mantendo a ordem", () => {
    const assunto = (slug: string, respondidas: number) => ({
      area: "MATEMATICA" as const,
      materia: rotulo("matematica"),
      assunto: rotulo(slug),
      aproveitamento: ap(respondidas),
    });
    const { medidos, semAmostra } = separarPorAmostra([assunto("a", 6), assunto("b", 1), assunto("c", 5), assunto("d", 0)]);

    expect(medidos.map((a) => a.assunto.slug)).toEqual(["a", "c"]);
    expect(semAmostra.map((a) => a.assunto.slug)).toEqual(["b", "d"]);
  });
});

describe("placarCurto", () => {
  it("assunto só com questões em branco não vira 0 de 0", () => {
    expect(placarCurto({ acertos: 0, respondidas: 0 })).toBe("só em branco");
    expect(placarCurto({ acertos: 1, respondidas: 2 })).toBe("1 de 2");
  });
});
