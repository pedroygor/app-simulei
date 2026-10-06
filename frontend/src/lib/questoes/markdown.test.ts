import { describe, expect, it } from "vitest";
import { escaparMarcadoresDeBloco } from "./markdown";

describe("escaparMarcadoresDeBloco", () => {
  it("número com ponto ou parêntese deixa de ser lista", () => {
    // Casos reais do banco: 2019 q148 tem as alternativas "9.", "12.", "89."...
    expect(escaparMarcadoresDeBloco("9.")).toBe("9\\.");
    expect(escaparMarcadoresDeBloco("112.")).toBe("112\\.");
    expect(escaparMarcadoresDeBloco("3) três")).toBe("3\\) três");
  });

  it("sinal no começo deixa de ser marcador e o sinal aparece", () => {
    expect(escaparMarcadoresDeBloco("+ 6,2 V.")).toBe("\\+ 6,2 V.");
    expect(escaparMarcadoresDeBloco("- 2")).toBe("\\- 2");
    expect(escaparMarcadoresDeBloco("# 1")).toBe("\\# 1");
  });

  it("não mexe no que o markdown já lê como texto", () => {
    for (const comum of ["9,5", "-2", "1.000 reais", "R$ 9.", "Reduzir 27%.", "2.5 m"]) {
      expect(escaparMarcadoresDeBloco(comum)).toBe(comum);
    }
  });

  it("vale pra cada linha", () => {
    expect(escaparMarcadoresDeBloco("a\n2. b")).toBe("a\n2\\. b");
  });
});
