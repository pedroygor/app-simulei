/**
 * Base da API. Fica num módulo próprio porque o cliente HTTP não é o único
 * consumidor: as imagens das questões vêm no markdown como caminho relativo
 * (`/imagens/ab/cd/hash.png`) e precisam ser resolvidas contra a MESMA base.
 */
export const URL_DA_API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * Resolve a URL de uma imagem de questão.
 *
 * O importador reescreve para caminho relativo o que conseguiu baixar, e
 * MANTÉM a URL original do enem.dev no que o allowlist recusou (ver seção 8.6
 * do CLAUDE.md). Então os dois formatos convivem no mesmo markdown, e tratar
 * só um deles quebraria metade das questões.
 *
 * Relativo precisa de prefixo: o front roda em :3000 e a API em :8080 —
 * sem isto o navegador pediria a imagem para o Next, que devolveria 404.
 */
export function resolverUrlDeImagem(src: string): string {
  if (/^https?:\/\//i.test(src)) {
    return src;
  }
  return `${URL_DA_API}${src.startsWith("/") ? src : `/${src}`}`;
}
