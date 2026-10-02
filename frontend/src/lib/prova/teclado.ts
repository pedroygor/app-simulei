/**
 * Navegação por teclado de um radiogroup, no padrão do WAI-ARIA (APG): setas
 * andam e dão a volta nas pontas, Home/End vão pros extremos. Devolve o índice
 * de destino, ou null se a tecla não é de navegação (aí o evento segue normal —
 * Tab, por exemplo, precisa SAIR do grupo).
 */
export function indiceDestino(atual: number, tecla: string, total: number): number | null {
  if (total === 0) return null;
  switch (tecla) {
    case "ArrowDown":
    case "ArrowRight":
      return (atual + 1) % total;
    case "ArrowUp":
    case "ArrowLeft":
      // + total antes do %: em JS, -1 % 5 é -1, não 4
      return (atual - 1 + total) % total;
    case "Home":
      return 0;
    case "End":
      return total - 1;
    default:
      return null;
  }
}

/**
 * Qual opção recebe o Tab ao entrar no grupo ("roving tabindex"): a marcada, ou
 * a primeira se nenhuma estiver. Só UMA opção fica na ordem do Tab — senão o
 * aluno teria que apertar Tab cinco vezes pra passar de uma questão.
 */
export function indiceFocavel(ids: number[], marcada: number | null): number {
  const indice = marcada === null ? -1 : ids.indexOf(marcada);
  return indice === -1 ? 0 : indice;
}
