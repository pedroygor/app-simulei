import { describe, expect, it } from "vitest";
import type { Taxonomia } from "@/lib/api/taxonomia";
import {
  campoDoFormulario,
  escolhaDaCascata,
  montarPedido,
  motivoParaRecusar,
  precisaDeIdioma,
  problemasDaMontagem,
  questoesDaEscolha,
  type EscolhaNaTela,
} from "./montagem";

// Recorte da taxonomia real, com contagens inventadas
const taxonomia: Taxonomia = [
  {
    area: "LINGUAGENS",
    materias: [
      { slug: "lingua-portuguesa", nome: "Língua Portuguesa", assuntos: [{ slug: "interpretacao-de-texto", nome: "Interpretação", questoes: 40 }] },
      { slug: "ingles", nome: "Inglês", assuntos: [{ slug: "interpretacao-ingles", nome: "Interpretação em inglês", questoes: 5 }] },
      { slug: "espanhol", nome: "Espanhol", assuntos: [{ slug: "interpretacao-espanhol", nome: "Interpretação em espanhol", questoes: 7 }] },
    ],
  },
  {
    area: "MATEMATICA",
    materias: [
      {
        slug: "matematica",
        nome: "Matemática",
        assuntos: [
          { slug: "estatistica", nome: "Estatística", questoes: 30 },
          { slug: "probabilidade", nome: "Probabilidade", questoes: 20 },
        ],
      },
    ],
  },
];

const matematica: EscolhaNaTela = { tipo: "MATERIA", valor: "matematica", rotulo: "Matemática" };
const probabilidade: EscolhaNaTela = { tipo: "ASSUNTO", valor: "probabilidade", rotulo: "Probabilidade" };
const linguagens: EscolhaNaTela = { tipo: "AREA", valor: "LINGUAGENS", rotulo: "Linguagens" };

describe("questoesDaEscolha", () => {
  it("soma os assuntos de uma área ou matéria", () => {
    expect(questoesDaEscolha(taxonomia, { tipo: "AREA", valor: "MATEMATICA" }, undefined)).toBe(50);
    expect(questoesDaEscolha(taxonomia, matematica, undefined)).toBe(50);
    expect(questoesDaEscolha(taxonomia, probabilidade, undefined)).toBe(20);
  });

  it("Linguagens sem idioma não conta a língua estrangeira; com idioma, só a escolhida", () => {
    expect(questoesDaEscolha(taxonomia, linguagens, undefined)).toBe(40);
    expect(questoesDaEscolha(taxonomia, linguagens, "INGLES")).toBe(45);
    expect(questoesDaEscolha(taxonomia, linguagens, "ESPANHOL")).toBe(47);
  });

  it("a matéria Inglês escolhida explicitamente conta sempre", () => {
    expect(questoesDaEscolha(taxonomia, { tipo: "MATERIA", valor: "ingles" }, undefined)).toBe(5);
  });
});

describe("precisaDeIdioma", () => {
  it("só com Linguagens inteira", () => {
    expect(precisaDeIdioma([linguagens])).toBe(true);
    expect(precisaDeIdioma([matematica, { tipo: "MATERIA", valor: "ingles" }])).toBe(false);
  });
});

describe("motivoParaRecusar", () => {
  it("assunto dentro de uma matéria já escolhida", () => {
    expect(motivoParaRecusar(taxonomia, [matematica], probabilidade, undefined)).toBe(
      "Probabilidade já está dentro de Matemática",
    );
  });

  it("matéria que engole um assunto já escolhido", () => {
    expect(motivoParaRecusar(taxonomia, [probabilidade], matematica, undefined)).toBe(
      "Probabilidade já está dentro de Matemática: tire uma das duas",
    );
  });

  it("a mesma escolha duas vezes", () => {
    expect(motivoParaRecusar(taxonomia, [probabilidade], probabilidade, undefined)).toBe("Probabilidade já está na prova");
  });

  it("Linguagens sem idioma não colide com a matéria Inglês; com inglês, colide", () => {
    const ingles: EscolhaNaTela = { tipo: "MATERIA", valor: "ingles", rotulo: "Inglês" };
    expect(motivoParaRecusar(taxonomia, [linguagens], ingles, undefined)).toBeNull();
    expect(motivoParaRecusar(taxonomia, [linguagens], ingles, "INGLES")).not.toBeNull();
  });

  it("escolhas sem assunto em comum entram", () => {
    expect(motivoParaRecusar(taxonomia, [matematica], linguagens, undefined)).toBeNull();
  });
});

describe("problemasDaMontagem", () => {
  it("mais escolhas que questões não cabe", () => {
    expect(problemasDaMontagem(5, Array(6).fill({}))).toEqual([
      "6 escolhas não cabem numa prova de 5 questões: cada uma leva pelo menos 1",
    ]);
  });

  it("só fixas precisam fechar o total", () => {
    expect(problemasDaMontagem(10, [{ quantidade: 4 }, { quantidade: 4 }])).toEqual([
      "As quantidades somam 8, mas a prova tem 10 questões",
    ]);
  });

  it("fixas que não deixam 1 pra cada livre", () => {
    expect(problemasDaMontagem(10, [{ quantidade: 10 }, {}])).toHaveLength(1);
  });

  it("pedido coerente não tem problema", () => {
    expect(problemasDaMontagem(10, [{ quantidade: 5 }, {}, {}])).toEqual([]);
    expect(problemasDaMontagem(10, [])).toEqual([]);
  });
});

describe("montarPedido", () => {
  it("tira o rótulo, ordena os anos e converte o tempo pra segundos", () => {
    expect(
      montarPedido({
        titulo: "  Treino  ",
        total: 10,
        anos: [2023, 2021],
        itens: [{ ...probabilidade, quantidade: 4 }, { tipo: "AREA", valor: "MATEMATICA", rotulo: "Matemática" }],
        tempoLimiteMin: 90,
      }),
    ).toEqual({
      titulo: "Treino",
      total: 10,
      anos: [2021, 2023],
      itens: [
        { tipo: "ASSUNTO", valor: "probabilidade", quantidade: 4 },
        { tipo: "AREA", valor: "MATEMATICA" },
      ],
      tempoLimiteSeg: 5400,
    });
  });

  it("idioma só vai quando Linguagens inteira está na prova", () => {
    const base = { titulo: "x", total: 5, anos: [], idioma: "INGLES" as const };
    expect(montarPedido({ ...base, itens: [linguagens] }).idioma).toBe("INGLES");
    expect(montarPedido({ ...base, itens: [matematica] }).idioma).toBeUndefined();
  });
});

describe("escolhaDaCascata", () => {
  it("a mais específica vence", () => {
    expect(escolhaDaCascata({ area: "MATEMATICA", materia: "matematica", assunto: "probabilidade" })).toEqual({
      tipo: "ASSUNTO",
      valor: "probabilidade",
    });
    expect(escolhaDaCascata({ area: "MATEMATICA" })).toEqual({ tipo: "AREA", valor: "MATEMATICA" });
    expect(escolhaDaCascata({})).toBeNull();
  });
});

describe("campoDoFormulario", () => {
  it("traduz a notação do Bean Validation pra do react-hook-form", () => {
    expect(campoDoFormulario("itens[2].quantidade")).toBe("itens.2.quantidade");
    expect(campoDoFormulario("anos[0]")).toBe("anos.0");
    expect(campoDoFormulario("tempoLimiteSeg")).toBe("tempoLimiteMin");
    expect(campoDoFormulario("titulo")).toBe("titulo");
  });
});
