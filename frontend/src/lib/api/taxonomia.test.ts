import { describe, expect, it } from "vitest";
import { areaDaMateria, assuntosDaMateria, materiasDaArea, type Taxonomia } from "./taxonomia";

// Recorte com a forma real de GET /taxonomia
const taxonomia: Taxonomia = [
  {
    area: "CIENCIAS_NATUREZA",
    materias: [
      {
        slug: "fisica",
        nome: "Física",
        assuntos: [
          { slug: "cinematica", nome: "Cinemática", questoes: 40 },
          { slug: "optica", nome: "Óptica", questoes: 25 },
        ],
      },
      { slug: "quimica", nome: "Química", assuntos: [{ slug: "estequiometria", nome: "Estequiometria", questoes: 30 }] },
    ],
  },
  {
    area: "MATEMATICA",
    materias: [
      { slug: "matematica", nome: "Matemática", assuntos: [{ slug: "probabilidade", nome: "Probabilidade", questoes: 87 }] },
    ],
  },
];

describe("materiasDaArea", () => {
  it("devolve só as matérias da área, na ordem da taxonomia", () => {
    expect(materiasDaArea(taxonomia, "CIENCIAS_NATUREZA").map((m) => m.slug)).toEqual(["fisica", "quimica"]);
  });

  it("sem área, devolve todas", () => {
    expect(materiasDaArea(taxonomia, undefined).map((m) => m.slug)).toEqual(["fisica", "quimica", "matematica"]);
  });

  it("devolve vazio enquanto a taxonomia não chegou", () => {
    expect(materiasDaArea(undefined, "MATEMATICA")).toEqual([]);
  });
});

describe("assuntosDaMateria", () => {
  it("devolve os assuntos da matéria, independente da área", () => {
    expect(assuntosDaMateria(taxonomia, "fisica").map((a) => a.slug)).toEqual(["cinematica", "optica"]);
  });

  it("sem matéria, nenhum assunto", () => {
    expect(assuntosDaMateria(taxonomia, undefined)).toEqual([]);
  });

  it("matéria inexistente dá lista vazia, sem estourar", () => {
    expect(assuntosDaMateria(taxonomia, "alquimia")).toEqual([]);
  });
});

describe("areaDaMateria", () => {
  it("acha a área a que a matéria pertence", () => {
    expect(areaDaMateria(taxonomia, "quimica")).toBe("CIENCIAS_NATUREZA");
    expect(areaDaMateria(taxonomia, "matematica")).toBe("MATEMATICA");
  });

  it("devolve undefined sem taxonomia ou com matéria desconhecida", () => {
    expect(areaDaMateria(undefined, "fisica")).toBeUndefined();
    expect(areaDaMateria(taxonomia, "alquimia")).toBeUndefined();
  });
});
