/**
 * Marcadores de BLOCO do markdown no começo de linha: lista numerada ("9.",
 * "1)"), lista com marcador ("-", "+", "*") e título ("#"). Só contam seguidos
 * de espaço ou fim de linha — "9,5" e "-2" já são texto comum pro markdown.
 */
const MARCADOR_DE_BLOCO = /^(\s*)(\d{1,9})([.)])(?=\s|$)|^(\s*)([-+*#])(?=\s|$)/gm;

/**
 * Pra texto que NUNCA é estrutura: alternativa é uma resposta, não uma lista.
 *
 * Medido no banco (05/10/2026): 325 alternativas, em 76 questões, são só um
 * número com ponto ("9.", "112."). O markdown lê isso como item de lista vazio
 * começando em 9, e o reset do Tailwind esconde o marcador — a alternativa
 * aparecia EM BRANCO, no detalhe e na prova. E 3 são "+ 6,2 V.": o "+" virava
 * marcador e o sinal da resposta sumia.
 *
 * A barra invertida é o escape do próprio markdown: "9\." renderiza "9.".
 */
export function escaparMarcadoresDeBloco(texto: string): string {
  return texto.replace(MARCADOR_DE_BLOCO, (_, recuoN, numero, sinal, recuoM, marcador) =>
    numero !== undefined ? `${recuoN}${numero}\\${sinal}` : `${recuoM}\\${marcador}`,
  );
}
