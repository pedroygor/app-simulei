import { describe, expect, it } from "vitest";
import { indiceDestino, indiceFocavel } from "./teclado";

describe("indiceDestino", () => {
  it("anda pra frente e dá a volta na última", () => {
    expect(indiceDestino(0, "ArrowDown", 5)).toBe(1);
    expect(indiceDestino(4, "ArrowDown", 5)).toBe(0);
    expect(indiceDestino(4, "ArrowRight", 5)).toBe(0);
  });

  it("anda pra trás e dá a volta na primeira", () => {
    expect(indiceDestino(3, "ArrowUp", 5)).toBe(2);
    expect(indiceDestino(0, "ArrowUp", 5)).toBe(4);
    expect(indiceDestino(0, "ArrowLeft", 5)).toBe(4);
  });

  it("Home e End vão pros extremos", () => {
    expect(indiceDestino(2, "Home", 5)).toBe(0);
    expect(indiceDestino(2, "End", 5)).toBe(4);
  });

  it("tecla que não navega devolve null pro evento seguir normal", () => {
    expect(indiceDestino(2, "Tab", 5)).toBeNull();
    expect(indiceDestino(2, "a", 5)).toBeNull();
    expect(indiceDestino(0, "ArrowDown", 0)).toBeNull();
  });
});

describe("indiceFocavel", () => {
  it("é a marcada, ou a primeira quando nada está marcado", () => {
    expect(indiceFocavel([10, 11, 12], 12)).toBe(2);
    expect(indiceFocavel([10, 11, 12], null)).toBe(0);
    // Marcada que não é deste grupo (dado velho): não some do Tab
    expect(indiceFocavel([10, 11, 12], 99)).toBe(0);
  });
});
