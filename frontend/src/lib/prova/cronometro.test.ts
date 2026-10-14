import { describe, expect, it } from "vitest";
import { avisoFalado, desvioDoRelogio, faseDoCronometro, formatarRestante, restanteMs } from "./cronometro";

const MIN = 60_000;

describe("desvio e restante", () => {
  it("desconta o relógio do celular adiantado", () => {
    // Celular 3 min À FRENTE do servidor: lê 10:03 quando o servidor diz 10:00
    const servidor = "2026-10-09T10:00:00Z";
    const celular = Date.parse("2026-10-09T10:03:00Z");
    const desvio = desvioDoRelogio(servidor, celular);
    expect(desvio).toBe(-3 * MIN);

    // Prazo 10:30. Pelo celular faltariam 27 min; pelo servidor, 30
    expect(restanteMs("2026-10-09T10:30:00Z", desvio, celular)).toBe(30 * MIN);
  });

  it("nunca fica negativo", () => {
    expect(restanteMs("2026-10-09T10:00:00Z", 0, Date.parse("2026-10-09T11:00:00Z"))).toBe(0);
  });
});

describe("formatarRestante", () => {
  it("mm:ss abaixo de 1 hora e h:mm:ss acima", () => {
    expect(formatarRestante(25 * MIN + 7_000)).toBe("25:07");
    expect(formatarRestante(5.5 * 60 * MIN)).toBe("5:30:00");
    expect(formatarRestante(60 * MIN)).toBe("1:00:00");
  });

  it("arredonda pra cima: zero só quando acabou", () => {
    expect(formatarRestante(400)).toBe("00:01");
    expect(formatarRestante(0)).toBe("00:00");
  });
});

describe("faseDoCronometro", () => {
  it("acabando nos 5 minutos finais, esgotado no zero", () => {
    expect(faseDoCronometro(5 * MIN + 1)).toBe("normal");
    expect(faseDoCronometro(5 * MIN)).toBe("acabando");
    expect(faseDoCronometro(1)).toBe("acabando");
    expect(faseDoCronometro(0)).toBe("esgotado");
  });
});

describe("avisoFalado", () => {
  it("fala só ao cruzar 5 min, 1 min e o fim", () => {
    expect(avisoFalado(5 * MIN + 500, 5 * MIN - 500)).toBe("Faltam 5 minutos");
    expect(avisoFalado(MIN + 500, MIN - 500)).toBe("Falta 1 minuto");
    expect(avisoFalado(500, 0)).toBe("Tempo esgotado");
  });

  it("não fala no meio do caminho nem repete", () => {
    expect(avisoFalado(20 * MIN, 20 * MIN - 1000)).toBeNull();
    expect(avisoFalado(4 * MIN, 4 * MIN - 1000)).toBeNull();
    expect(avisoFalado(0, 0)).toBeNull();
  });

  it("aba em segundo plano que pula vários marcos anuncia o último", () => {
    expect(avisoFalado(6 * MIN, 30_000)).toBe("Falta 1 minuto");
  });
});
