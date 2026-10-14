import { describe, expect, it } from "vitest";
import type { ProvaDoHistorico } from "@/lib/api/simulados";
import { destinoDaProva, proximaPagina, resumoDaProva } from "./historico";

function prova(mudanca: Partial<ProvaDoHistorico>): ProvaDoHistorico {
  return {
    simuladoId: "s1",
    titulo: "Treino",
    criadoEm: "2026-10-06T10:00:00Z",
    situacao: "NAO_INICIADA",
    tentativaId: null,
    iniciadoEm: null,
    finalizadoEm: null,
    questoes: 10,
    respondidas: 0,
    placar: null,
    ...mudanca,
  };
}

describe("resumoDaProva", () => {
  it("entregue mostra a nota", () => {
    const placar = { questoes: 10, respondidas: 9, acertos: 7, emBranco: 1, percentual: 70 };
    expect(resumoDaProva(prova({ situacao: "ENTREGUE", tentativaId: "t1", placar }))).toBe("7 de 10 · 70%");
  });

  it("entregue sem questão a corrigir não vira 0%", () => {
    const placar = { questoes: 0, respondidas: 0, acertos: 0, emBranco: 0, percentual: null };
    expect(resumoDaProva(prova({ situacao: "ENTREGUE", tentativaId: "t1", placar }))).toBe("Sem questões a corrigir");
  });

  it("em andamento mostra só quantas respondeu (acerto seria o gabarito)", () => {
    expect(resumoDaProva(prova({ situacao: "EM_ANDAMENTO", tentativaId: "t1", respondidas: 3 }))).toBe(
      "3 de 10 respondidas",
    );
  });

  it("não iniciada mostra o tamanho", () => {
    expect(resumoDaProva(prova({}))).toBe("10 questões");
    expect(resumoDaProva(prova({ questoes: 1 }))).toBe("1 questão");
  });
});

describe("destinoDaProva", () => {
  it("com tentativa abre a tela dela; sem tentativa não há link", () => {
    expect(destinoDaProva(prova({ tentativaId: "t1", situacao: "ENTREGUE" }))).toBe("/tentativas/t1");
    expect(destinoDaProva(prova({}))).toBeNull();
  });
});

describe("proximaPagina", () => {
  it("avança enquanto houver página e para na última", () => {
    expect(proximaPagina({ itens: [], pagina: 0, totalDeItens: 45, totalDePaginas: 3 })).toBe(1);
    expect(proximaPagina({ itens: [], pagina: 2, totalDeItens: 45, totalDePaginas: 3 })).toBeUndefined();
    expect(proximaPagina({ itens: [], pagina: 0, totalDeItens: 0, totalDePaginas: 0 })).toBeUndefined();
  });
});
