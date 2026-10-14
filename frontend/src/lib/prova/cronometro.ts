/**
 * O relógio da prova (3d). Quem manda no tempo é o SERVIDOR: o prazo vem dele e
 * a resposta depois do prazo é recusada lá. Aqui só se mostra quanto falta.
 *
 * O relógio do celular pode estar errado (ou ter sido ajustado de propósito).
 * Por isso a API manda `agora` (a hora dela) junto com a prova, e a tela mede
 * a diferença UMA vez — um relógio adiantado 3 min continua adiantado 3 min.
 */

const SEGUNDO = 1000;
const MINUTO = 60 * SEGUNDO;

/** Quanto o servidor está à frente do celular (negativo = atrás), em ms. */
export function desvioDoRelogio(agoraDoServidor: string, recebidoEmLocal: number): number {
  return Date.parse(agoraDoServidor) - recebidoEmLocal;
}

/** Quanto falta pelo relógio do servidor; nunca negativo. */
export function restanteMs(prazo: string, desvio: number, agoraLocal: number): number {
  return Math.max(0, Date.parse(prazo) - (agoraLocal + desvio));
}

/**
 * mm:ss, ou h:mm:ss a partir de 1 hora (o ENEM inteiro tem 5h30). Arredonda
 * PRA CIMA: mostrar 00:00 com 0,4 s ainda sobrando faria o aluno largar a
 * questão achando que acabou — o zero só aparece quando acabou mesmo.
 */
export function formatarRestante(ms: number): string {
  const total = Math.ceil(ms / SEGUNDO);
  const horas = Math.floor(total / 3600);
  const minutos = Math.floor((total % 3600) / 60);
  const segundos = total % 60;
  const dois = (n: number) => String(n).padStart(2, "0");
  return horas > 0 ? `${horas}:${dois(minutos)}:${dois(segundos)}` : `${dois(minutos)}:${dois(segundos)}`;
}

export type FaseDoCronometro = "normal" | "acabando" | "esgotado";

/** "Acabando" nos 5 minutos finais. */
export function faseDoCronometro(ms: number): FaseDoCronometro {
  if (ms <= 0) return "esgotado";
  return ms <= 5 * MINUTO ? "acabando" : "normal";
}

const MARCOS = [
  { limite: 5 * MINUTO, texto: "Faltam 5 minutos" },
  { limite: MINUTO, texto: "Falta 1 minuto" },
  { limite: 0, texto: "Tempo esgotado" },
];

/**
 * O que o leitor de tela anuncia: só quando o tempo CRUZA 5 min, 1 min e o fim.
 * Anunciar todo segundo tornaria a prova impossível pra quem usa leitor; e quem
 * abre a prova já com 3 min sobrando não ouve "faltam 5" (não cruzou nada).
 * Se vários marcos foram cruzados de uma vez (aba em segundo plano), vale o último.
 */
export function avisoFalado(msAnterior: number, msAtual: number): string | null {
  let aviso: string | null = null;
  for (const { limite, texto } of MARCOS) {
    if (msAnterior > limite && msAtual <= limite) aviso = texto;
  }
  return aviso;
}
