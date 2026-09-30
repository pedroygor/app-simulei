import { describe, expect, it } from "vitest";
import {
  comOpcaoSelecionada,
  lerFiltroDaUrl,
  montarOpcoesDeAno,
  montarUrlDeQuestoes,
  mudarFiltro,
  temFiltroAtivo,
} from "./filtro-da-url";

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

  it("lê matéria e assunto como slug", () => {
    expect(lerFiltroDaUrl({ materia: "fisica", assunto: "eletrodinamica" })).toMatchObject({
      materia: "fisica",
      assunto: "eletrodinamica",
    });
  });

  it("descarta slug fora do formato do backend", () => {
    // Iriam pro Spring e voltariam 400 (@Pattern do BuscarQuestoesFiltro)
    expect(lerFiltroDaUrl({ materia: "Física" }).materia).toBeUndefined();
    expect(lerFiltroDaUrl({ assunto: "funcao do 1 grau" }).assunto).toBeUndefined();
    expect(lerFiltroDaUrl({ assunto: "-probabilidade" }).assunto).toBeUndefined();
    expect(lerFiltroDaUrl({ assunto: "a".repeat(61) }).assunto).toBeUndefined();
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
      materia: "quimica",
      assunto: "estequiometria",
      pagina: 4,
    };

    const url = new URL(montarUrlDeQuestoes(original), "http://x");
    const devolta = lerFiltroDaUrl(Object.fromEntries(url.searchParams));

    expect(devolta).toEqual(original);
  });
});

describe("mudarFiltro — a hierarquia área → matéria → assunto fica coerente", () => {
  const completo = {
    area: "CIENCIAS_NATUREZA" as const,
    materia: "fisica",
    assunto: "eletrodinamica",
    anoInicial: 2020,
    pagina: 3,
  };

  it("trocar a área limpa matéria e assunto", () => {
    // Sem isto: ?area=MATEMATICA&materia=fisica, lista sempre vazia
    expect(mudarFiltro(completo, { area: "MATEMATICA" })).toMatchObject({
      area: "MATEMATICA",
      materia: undefined,
      assunto: undefined,
    });
  });

  it("trocar a matéria limpa só o assunto", () => {
    expect(mudarFiltro(completo, { materia: "quimica" })).toMatchObject({
      area: "CIENCIAS_NATUREZA",
      materia: "quimica",
      assunto: undefined,
    });
  });

  it("escolher a matéria junto com a área dela não apaga a matéria", () => {
    // O filtro manda { area, materia } ao escolher uma matéria
    const semArea = { pagina: 0 };
    expect(mudarFiltro(semArea, { area: "MATEMATICA", materia: "matematica" })).toMatchObject({
      area: "MATEMATICA",
      materia: "matematica",
    });
  });

  it("mudança que não mexe na hierarquia preserva matéria e assunto", () => {
    expect(mudarFiltro(completo, { anoInicial: 2015, anoFinal: 2015 })).toMatchObject({
      materia: "fisica",
      assunto: "eletrodinamica",
    });
  });

  it("repetir a mesma área não limpa nada", () => {
    expect(mudarFiltro(completo, { area: "CIENCIAS_NATUREZA" }).assunto).toBe("eletrodinamica");
  });

  it("toda mudança volta pra primeira página", () => {
    expect(mudarFiltro(completo, { assunto: "optica" }).pagina).toBe(0);
  });
});

describe("temFiltroAtivo", () => {
  it("considera matéria e assunto", () => {
    // Bug anterior: com só a matéria filtrada, o "Limpar filtros" sumia
    expect(temFiltroAtivo({ materia: "fisica" })).toBe(true);
    expect(temFiltroAtivo({ assunto: "optica" })).toBe(true);
  });

  it("página sozinha não é filtro", () => {
    expect(temFiltroAtivo({ pagina: 4 })).toBe(false);
  });
});

describe("comOpcaoSelecionada", () => {
  const opcoes = [{ valor: "fisica", rotulo: "Física" }];

  it("não altera a lista quando o selecionado está nela", () => {
    expect(comOpcaoSelecionada(opcoes, "fisica")).toEqual(opcoes);
    expect(comOpcaoSelecionada(opcoes, undefined)).toEqual(opcoes);
  });

  it("inclui o selecionado ausente, rotulado pelo slug", () => {
    // Taxonomia ainda carregando, ou slug de link antigo
    expect(comOpcaoSelecionada([], "fisica")).toEqual([{ valor: "fisica", rotulo: "fisica" }]);
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
