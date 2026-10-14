import { describe, expect, it, vi } from "vitest";
import { FilaDeSalvamento } from "./fila-de-salvamento";

/** Envio controlado pelo teste: cada chamada fica pendente até resolver/rejeitar. */
function envioControlado() {
  const chamadas: { questao: number; alternativa: number | null; resolver: () => void; rejeitar: (e: unknown) => void }[] =
    [];
  const envio = vi.fn(
    (questao: number, alternativa: number | null) =>
      new Promise<void>((resolver, rejeitar) => chamadas.push({ questao, alternativa, resolver, rejeitar })),
  );
  return { envio, chamadas };
}

// Deixa as continuações das promessas (o código depois do await) rodarem
const microtarefas = () => new Promise((r) => setTimeout(r, 0));

describe("FilaDeSalvamento", () => {
  it("mostra a marcação na hora e a dá como salva quando o servidor confirma", async () => {
    const { envio, chamadas } = envioControlado();
    const fila = new FilaDeSalvamento(new Map(), envio);

    fila.marcar(1, 10);
    expect(fila.retrato().marcadas.get(1)).toBe(10);
    expect(fila.retrato().situacoes.get(1)).toBe("salvando");

    chamadas[0].resolver();
    await microtarefas();
    expect(fila.retrato().situacoes.has(1)).toBe(false);
    expect(fila.pendentes()).toBe(0);
  });

  it("toques rápidos: um envio por vez e o último valor sempre chega por último", async () => {
    const { envio, chamadas } = envioControlado();
    const fila = new FilaDeSalvamento(new Map(), envio);

    fila.marcar(1, 10);
    fila.marcar(1, 11);
    fila.marcar(1, 12);
    // Só o primeiro saiu; os outros esperam
    expect(envio).toHaveBeenCalledTimes(1);

    chamadas[0].resolver();
    await microtarefas();
    // O intermediário (11) nunca é enviado
    expect(chamadas.map((c) => c.alternativa)).toEqual([10, 12]);

    chamadas[1].resolver();
    await microtarefas();
    expect(envio).toHaveBeenCalledTimes(2);
    expect(fila.retrato().marcadas.get(1)).toBe(12);
    expect(fila.pendentes()).toBe(0);
  });

  it("questões diferentes salvam em paralelo", () => {
    const { envio } = envioControlado();
    const fila = new FilaDeSalvamento(new Map(), envio);

    fila.marcar(1, 10);
    fila.marcar(2, 20);

    expect(envio).toHaveBeenCalledTimes(2);
    expect(fila.pendentes()).toBe(2);
  });

  it("voltar ao valor que está em voo não gera envio extra", async () => {
    const { envio, chamadas } = envioControlado();
    const fila = new FilaDeSalvamento(new Map(), envio);

    fila.marcar(1, 10);
    fila.marcar(1, 11);
    fila.marcar(1, 10);
    chamadas[0].resolver();
    await microtarefas();

    expect(envio).toHaveBeenCalledTimes(1);
  });

  it("repetir o que já está salvo não chama o servidor", () => {
    const { envio } = envioControlado();
    const fila = new FilaDeSalvamento(new Map([[1, 10]]), envio);

    fila.marcar(1, 10);

    expect(envio).not.toHaveBeenCalled();
  });

  it("falha mantém a marcação, avisa e tentar de novo reenvia", async () => {
    const { envio, chamadas } = envioControlado();
    const aoFalhar = vi.fn();
    const fila = new FilaDeSalvamento(new Map(), envio, aoFalhar);

    fila.marcar(1, 10);
    chamadas[0].rejeitar(new Error("sem rede"));
    await microtarefas();

    expect(fila.retrato().marcadas.get(1)).toBe(10);
    expect(fila.retrato().situacoes.get(1)).toBe("erro");
    expect(fila.pendentes()).toBe(1);
    expect(aoFalhar).toHaveBeenCalledOnce();

    fila.tentarDeNovo(1);
    expect(chamadas[1].alternativa).toBe(10);
    chamadas[1].resolver();
    await microtarefas();
    expect(fila.pendentes()).toBe(0);
  });

  it("desmarcar envia null", () => {
    const { envio, chamadas } = envioControlado();
    const fila = new FilaDeSalvamento(new Map([[1, 10]]), envio);

    fila.marcar(1, null);

    expect(chamadas[0].alternativa).toBeNull();
    expect(fila.retrato().marcadas.get(1)).toBeNull();
  });

  it("cada mudança publica um retrato NOVO (o React compara por referência)", () => {
    const { envio } = envioControlado();
    const fila = new FilaDeSalvamento(new Map(), envio);
    const ouvinte = vi.fn();
    fila.assinar(ouvinte);
    const antes = fila.retrato();

    fila.marcar(1, 10);

    expect(fila.retrato()).not.toBe(antes);
    expect(ouvinte).toHaveBeenCalled();
  });

  it("aguardarEnvios espera o envio em voo terminar", async () => {
    const { envio, chamadas } = envioControlado();
    const fila = new FilaDeSalvamento(new Map(), envio);
    fila.marcar(1, 10);

    let terminou = false;
    const espera = fila.aguardarEnvios(5000).then(() => (terminou = true));
    await microtarefas();
    expect(terminou).toBe(false);

    chamadas[0].resolver();
    await espera;
    expect(terminou).toBe(true);
  });

  it("aguardarEnvios desiste no limite (rede caída não trava a entrega)", async () => {
    vi.useFakeTimers();
    try {
      const { envio } = envioControlado();
      const fila = new FilaDeSalvamento(new Map(), envio);
      fila.marcar(1, 10);

      const espera = fila.aguardarEnvios(8000);
      vi.advanceTimersByTime(8000);
      await espera;
      expect(fila.pendentes()).toBe(1);
    } finally {
      vi.useRealTimers();
    }
  });

  it("aguardarEnvios sem nada em voo resolve na hora", async () => {
    const { envio } = envioControlado();
    await new FilaDeSalvamento(new Map(), envio).aguardarEnvios(8000);
  });
});
