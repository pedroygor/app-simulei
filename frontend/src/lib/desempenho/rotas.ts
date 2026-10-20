/** A página da evolução de um assunto. O slug é da taxonomia (só letras minúsculas, números e hífen). */
export function rotaDoAssunto(slug: string): string {
  return `/desempenho/assuntos/${slug}`;
}
