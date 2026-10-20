import { describe, expect, it } from "vitest";
import type { Taxonomia } from "@/lib/api/taxonomia";
import { pedidoDeTreino, questoesNoBanco } from "./treino";

const probabilidade = { slug: "probabilidade", nome: "Probabilidade" };

describe("pedidoDeTreino", () => {
  it("10 questões do assunto, sem cronômetro e sem filtro de ano", () => {
    expect(pedidoDeTreino(probabilidade, 87)).toEqual({
      titulo: "Treino: Probabilidade",
      total: 10,
      anos: [],
      itens: [{ tipo: "ASSUNTO", valor: "probabilidade" }],
    });
  });

  it("assunto pequeno treina com o que tem", () => {
    expect(pedidoDeTreino(probabilidade, 6)?.total).toBe(6);
    expect(pedidoDeTreino(probabilidade, 5)?.total).toBe(5);
  });

  it("abaixo do mínimo da montagem não há treino", () => {
    expect(pedidoDeTreino(probabilidade, 4)).toBeNull();
    expect(pedidoDeTreino(probabilidade, 0)).toBeNull();
  });
});

describe("questoesNoBanco", () => {
  const taxonomia: Taxonomia = [
    {
      area: "MATEMATICA",
      materias: [{ slug: "matematica", nome: "Matemática", assuntos: [{ slug: "probabilidade", nome: "Probabilidade", questoes: 87 }] }],
    },
  ];

  it("acha o assunto em qualquer matéria; desconhecido vale 0", () => {
    expect(questoesNoBanco(taxonomia, "probabilidade")).toBe(87);
    expect(questoesNoBanco(taxonomia, "nao-existe")).toBe(0);
  });
});
