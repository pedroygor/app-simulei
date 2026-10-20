import { describe, expect, it } from "vitest";
import type { Aproveitamento, Evolucao } from "@/lib/api/desempenho";
import { progressoDaProva, quandoTermina, tendenciaSemanal } from "./painel";

const ap = (respondidas: number, acertos: number): Aproveitamento => ({
  respondidas,
  acertos,
  emBranco: 0,
  percentual: respondidas === 0 ? null : Math.floor((acertos * 100) / respondidas),
  amostraSuficiente: respondidas >= 5,
  pertoDoChute: false,
});

const evolucao = (...gerais: Aproveitamento[]): Evolucao => ({
  amostraMinima: 5,
  assunto: null,
  semanas: gerais.map((geral, i) => ({ inicio: `2026-10-${String(5 + i * 7).padStart(2, "0")}`, geral, porArea: [] })),
});

describe("tendenciaSemanal", () => {
  it("compara as duas últimas semanas quando as duas têm amostra", () => {
    expect(tendenciaSemanal(evolucao(ap(10, 1), ap(10, 4), ap(10, 6)))).toEqual({ atual: 60, anterior: 40, diferenca: 20 });
    expect(tendenciaSemanal(evolucao(ap(10, 6), ap(10, 3)))?.diferenca).toBe(-30);
  });

  it("sem amostra em qualquer uma das duas, não há tendência", () => {
    expect(tendenciaSemanal(evolucao(ap(10, 4), ap(3, 3)))).toBeNull();
    expect(tendenciaSemanal(evolucao(ap(4, 1), ap(10, 6)))).toBeNull();
    expect(tendenciaSemanal(evolucao(ap(10, 4), ap(0, 0)))).toBeNull();
    expect(tendenciaSemanal(evolucao(ap(10, 4)))).toBeNull();
  });
});

describe("progressoDaProva", () => {
  it("diz quantas foram respondidas, sem acertos", () => {
    expect(progressoDaProva({ questoes: 45, respondidas: 12 })).toBe("12 de 45 respondidas");
    expect(progressoDaProva({ questoes: 1, respondidas: 0 })).toBe("0 de 1 respondida");
  });
});

describe("quandoTermina", () => {
  it("hoje mostra só a hora, no fuso pedido", () => {
    // 18:40 UTC = 15:40 em Brasília
    expect(quandoTermina("2026-10-20T18:40:00Z", "2026-10-20T15:00:00Z", "America/Sao_Paulo")).toBe("termina às 15:40");
  });

  it("outro dia mostra a data junto", () => {
    expect(quandoTermina("2026-10-21T13:05:00Z", "2026-10-20T15:00:00Z", "America/Sao_Paulo")).toBe("termina em 21/10 às 10:05");
  });

  it("o dia é o do fuso, não o de UTC", () => {
    // 01:30 UTC do dia 21 ainda é 22:30 do dia 20 em Brasília
    expect(quandoTermina("2026-10-21T01:30:00Z", "2026-10-20T15:00:00Z", "America/Sao_Paulo")).toBe("termina às 22:30");
  });
});
