import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { TextoDaQuestao } from "./texto-da-questao";

const render = (texto: string | null) => renderToStaticMarkup(<TextoDaQuestao texto={texto} />);

/**
 * String REAL da alternativa B de 2009 q105, copiada da resposta da enem.dev.
 * Não é caso inventado: é o que a fonte entrega hoje.
 */
const ALTERNATIVA_REAL_COM_HTML =
  "Cuja linguagem se caracteriza por ser rápida e clara, que facilita a " +
  "compreensão, como se percebe na fala do segundo quadrinho: " +
  "“</DIV> </SPAN> <BR CLEAR = ALL> < BR> <BR> <SCRIPT>”.";

describe("TextoDaQuestao — contenção de HTML da fonte", () => {
  it("não transforma o <SCRIPT> real da enem.dev em tag", () => {
    const html = render(ALTERNATIVA_REAL_COM_HTML);

    // A asserção que importa: nenhuma tag de verdade saiu daqui
    expect(html.toLowerCase()).not.toContain("<script");
    expect(html.toLowerCase()).not.toContain("<br");

    // O <div> da fonte não pode ter virado elemento. Contar é mais honesto que
    // procurar a substring "</div": o wrapper deste componente é um div, e
    // buscá-la crua acusaria a nossa própria tag de fechamento.
    expect(html.match(/<div/gi)).toHaveLength(1);
    expect(html.match(/<\/div/gi)).toHaveLength(1);

    // ...e o texto continua legível pro aluno, escapado. Em MAIÚSCULO, como
    // veio da fonte: escapar não altera o conteúdo, só o desarma.
    expect(html).toContain("&lt;SCRIPT&gt;");
    expect(html).toContain("&lt;/DIV&gt;");
    expect(html).toContain("linguagem se caracteriza");
  });

  it("escapa <script> injetado com payload executável", () => {
    const html = render('Antes <script>alert("xss")</script> depois');

    expect(html.toLowerCase()).not.toContain("<script");
    expect(html).toContain("Antes");
    expect(html).toContain("depois");
  });

  it("não deixa passar handler inline de evento", () => {
    const html = render('<img src=x onerror="alert(1)"> texto');

    // `onerror="` seria ATRIBUTO. Escapado (`onerror=&quot;`) é só texto na
    // tela — inerte. A distinção é toda a diferença, então a asserção precisa
    // ser sobre a forma não escapada, não sobre a palavra solta.
    expect(html).not.toContain('onerror="');
    expect(html).toContain("onerror=&quot;");
    expect(html.toLowerCase()).not.toContain("<img");
  });

  it("barra javascript: no src de imagem markdown", () => {
    const html = render("![](javascript:alert(1))");

    expect(html).not.toContain("javascript:alert");
  });

  it("neutraliza link com javascript:, preservando o texto", () => {
    // <a> não está na allowlist: o elemento cai, o texto fica
    const html = render("[clique aqui](javascript:alert(1))");

    expect(html).not.toContain("<a");
    expect(html).not.toContain("javascript:");
    expect(html).toContain("clique aqui");
  });
});

describe("TextoDaQuestao — renderização do que a fonte realmente usa", () => {
  it("renderiza negrito e itálico", () => {
    const html = render("um **negrito** e um _itálico_");

    expect(html).toContain("<strong>negrito</strong>");
    expect(html).toContain("<em>itálico</em>");
  });

  it("resolve imagem relativa contra a base da API", () => {
    const html = render("![](/imagens/3b/0e/3b0e4450.png)");

    expect(html).toContain('src="http://localhost:8080/imagens/3b/0e/3b0e4450.png"');
  });

  it("preserva imagem absoluta que ficou apontando pra fonte", () => {
    // As 7 recusadas pelo allowlist de magic bytes mantêm a URL do enem.dev
    const html = render("![](https://enem.dev/broken-image.svg)");

    expect(html).toContain('src="https://enem.dev/broken-image.svg"');
  });

  it("dá rótulo à imagem sem alt, que é o caso normal da fonte", () => {
    const html = render("![](/imagens/ab/cd/hash.png)");

    expect(html).toContain('alt="Imagem da questão"');
  });

  it("preserva o alt quando a fonte fornece um", () => {
    const html = render("![Gráfico de barras](/imagens/ab/cd/hash.png)");

    expect(html).toContain('alt="Gráfico de barras"');
  });

  it("não renderiza nada para texto ausente ou em branco", () => {
    // 5% das questões não têm enunciado (migration V3) — a tela chama isto
    // com null e não pode quebrar nem deixar caixa vazia na página
    expect(render(null)).toBe("");
    expect(render("   ")).toBe("");
  });
});
