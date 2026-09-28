import { describe, expect, it } from "vitest";
import { lerFiltroDaUrl, montarOpcoesDeAno, montarUrlDeQuestoes } from "./filtro-da-url";

describe("lerFiltroDaUrl — a URL é entrada de usuário", () => {
  it("lê os filtros válidos", () => {
    expect(lerFiltroDaUrl({ area: "MATEMATICA", idioma: "INGLES", anoInicial: "2015" })).toMatchObject(
      { area: "MATEMATICA", idioma: "INGLES", anoInicial: 2015 },
    );
  });

  it("descarta área e idioma fora do enum em vez de repassar", () => {
    // `?area=BANANA` chegaria no Spring e voltaria 400: a tela mostraria erro
    // de validação a quem só queria abrir a página
    const filtro = lerFiltroDaUrl({ area: "BANANA", idioma: "KLINGON" });

    expect(filtro.area).toBeUndefined();
    expect(filtro.idioma).toBeUndefined();
  });

  it("descarta ano fora do intervalo aceito", () => {
    expect(lerFiltroDaUrl({ anoInicial: "1500" }).anoInicial).toBeUndefined();
    expect(lerFiltroDaUrl({ anoInicial: "2099" }).anoInicial).toBeUndefined();
    expect(lerFiltroDaUrl({ anoInicial: "abc" }).anoInicial).toBeUndefined();
    expect(lerFiltroDaUrl({ anoInicial: "2015.5" }).anoInicial).toBeUndefined();
  });

  it("aceita os limites do intervalo", () => {
    expect(lerFiltroDaUrl({ anoInicial: "2009" }).anoInicial).toBe(2009);
    expect(lerFiltroDaUrl({ anoFinal: "2030" }).anoFinal).toBe(2030);
  });

  it("converte a página de 1-based da URL pra 0-based do Spring Data", () => {
    expect(lerFiltroDaUrl({}).pagina).toBe(0);
    expect(lerFiltroDaUrl({ pagina: "1" }).pagina).toBe(0);
    expect(lerFiltroDaUrl({ pagina: "3" }).pagina).toBe(2);
  });

  it("trata página absurda como a primeira", () => {
    // ?pagina=-5 viraria page=-6 e estouraria no backend
    expect(lerFiltroDaUrl({ pagina: "-5" }).pagina).toBe(0);
    expect(lerFiltroDaUrl({ pagina: "0" }).pagina).toBe(0);
    expect(lerFiltroDaUrl({ pagina: "abc" }).pagina).toBe(0);
  });

  it("usa o primeiro valor quando o parâmetro vem repetido", () => {
    // ?area=X&area=Y chega como array
    expect(lerFiltroDaUrl({ area: ["MATEMATICA", "LINGUAGENS"] }).area).toBe("MATEMATICA");
  });

  it("ignora matéria em branco", () => {
    expect(lerFiltroDaUrl({ materia: "   " }).materia).toBeUndefined();
  });
});

describe("montarUrlDeQuestoes — precisa ser o inverso da leitura", () => {
  it("omite tudo que está vazio", () => {
    expect(montarUrlDeQuestoes({})).toBe("/questoes");
    expect(montarUrlDeQuestoes({ pagina: 0 })).toBe("/questoes");
  });

  it("volta a página pra 1-based na URL", () => {
    expect(montarUrlDeQuestoes({ pagina: 2 })).toBe("/questoes?pagina=3");
  });

  it("faz a ida e volta sem perder nem alterar filtro", () => {
    // Se leitura e escrita discordarem, o usuário clica num filtro e a tela
    // volta com outro estado. Este teste é o que amarra as duas.
    const original = {
      area: "CIENCIAS_NATUREZA" as const,
      idioma: "ESPANHOL" as const,
      anoInicial: 2012,
      anoFinal: 2019,
      materia: "química",
      pagina: 4,
    };

    const url = new URL(montarUrlDeQuestoes(original), "http://x");
    const devolta = lerFiltroDaUrl(Object.fromEntries(url.searchParams));

    expect(devolta).toEqual(original);
  });
});

describe("montarOpcoesDeAno", () => {
  it("oferece só os anos disponíveis, do mais recente pro mais antigo", () => {
    expect(montarOpcoesDeAno([2021, 2023, 2022], undefined)).toEqual([2023, 2022, 2021]);
  });

  it("mantém o ano selecionado enquanto a lista ainda não chegou", () => {
    // Sem isto o Select renderiza em branco com o filtro de 2022 ativo
    expect(montarOpcoesDeAno(undefined, 2022)).toEqual([2022]);
  });

  it("mantém um ano selecionado que não está entre os disponíveis", () => {
    // ?anoInicial=2025 de link antigo: o campo mostra 2025 e a lista vazia
    // faz sentido, em vez de campo em branco com lista vazia
    expect(montarOpcoesDeAno([2023, 2022], 2025)).toEqual([2025, 2023, 2022]);
  });

  it("não duplica o selecionado quando ele já está na lista", () => {
    expect(montarOpcoesDeAno([2023, 2022], 2022)).toEqual([2023, 2022]);
  });

  it("devolve vazio sem lista e sem seleção", () => {
    expect(montarOpcoesDeAno(undefined, undefined)).toEqual([]);
  });
});
