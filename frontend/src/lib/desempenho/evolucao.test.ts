import { describe, expect, it } from "vitest";
import type { Aproveitamento, Evolucao } from "@/lib/api/desempenho";
import { leituraDaColuna, rotuloDaSemana, serieGeral, seriesPorArea, ultimaComDado } from "./evolucao";

const ap = (respondidas: number, acertos: number): Aproveitamento => ({
  respondidas,
  acertos,
  emBranco: 0,
  percentual: respondidas === 0 ? null : Math.floor((acertos * 100) / respondidas),
  amostraSuficiente: respondidas >= 5,
  pertoDoChute: false,
});

const evolucao: Evolucao = {
  amostraMinima: 5,
  semanas: [
    { inicio: "2026-09-28", geral: ap(0, 0), porArea: [] },
    {
      inicio: "2026-10-05",
      geral: ap(8, 6),
      porArea: [
        { area: "MATEMATICA", nome: "Matemática", aproveitamento: ap(5, 4) },
        { area: "CIENCIAS_NATUREZA", nome: "Ciências da Natureza", aproveitamento: ap(3, 2) },
      ],
    },
    { inicio: "2026-10-12", geral: ap(4, 1), porArea: [{ area: "MATEMATICA", nome: "Matemática", aproveitamento: ap(4, 1) }] },
    { inicio: "2026-10-19", geral: ap(0, 0), porArea: [] },
  ],
};

describe("rotuloDaSemana", () => {
  it("lê a data como texto, sem cair no dia anterior pelo fuso", () => {
    expect(rotuloDaSemana("2026-10-13")).toBe("13/10");
    expect(rotuloDaSemana("2026-01-05")).toBe("05/01");
  });
});

describe("serieGeral", () => {
  it("semana sem resposta vira coluna vazia, não 0%", () => {
    expect(serieGeral(evolucao).map((c) => c.aproveitamento?.percentual ?? null)).toEqual([null, 75, 25, null]);
  });
});

describe("seriesPorArea", () => {
  it("só as áreas com resposta na janela, na ordem do ENEM, com buraco onde a área não teve prova", () => {
    const series = seriesPorArea(evolucao);

    expect(series.map((s) => s.area)).toEqual(["CIENCIAS_NATUREZA", "MATEMATICA"]);
    expect(series[0].colunas.map((c) => c.aproveitamento?.respondidas ?? null)).toEqual([null, 3, null, null]);
    expect(series[1].colunas.map((c) => c.aproveitamento?.respondidas ?? null)).toEqual([null, 5, 4, null]);
  });
});

describe("ultimaComDado e leituraDaColuna", () => {
  it("a leitura começa na última semana com resposta e diz quando a amostra é pequena", () => {
    const colunas = serieGeral(evolucao);
    expect(ultimaComDado(colunas)).toBe(2);
    expect(ultimaComDado(serieGeral({ ...evolucao, semanas: [evolucao.semanas[0]] }))).toBeNull();

    expect(leituraDaColuna(colunas[1])).toBe("Semana de 05/10: 75% · 6 de 8 certas");
    expect(leituraDaColuna(colunas[2])).toBe("Semana de 12/10: 25% · 1 de 4 certas (poucas respostas)");
    expect(leituraDaColuna(colunas[3])).toBe("Semana de 19/10: sem prova entregue");
  });
});
