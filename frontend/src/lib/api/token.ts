/**
 * Access token guardado em MEMÓRIA — nunca em localStorage ou sessionStorage.
 *
 * Storage do navegador é legível por qualquer script da página: um XSS (ou uma
 * dependência comprometida) lê o token e o usa até expirar. Em memória, o
 * roubo exige execução no momento certo, e o token some quando a aba fecha.
 *
 * O preço é que um F5 perde o token. Quem resolve isso é o refresh silencioso
 * na inicialização (ver AuthProvider): o cookie httpOnly sobrevive ao reload e
 * troca por um access token novo. O JS nunca enxerga esse cookie.
 */
let accessToken: string | null = null;

export function guardarAccessToken(token: string | null) {
  accessToken = token;
}

export function lerAccessToken() {
  return accessToken;
}

export function limparAccessToken() {
  accessToken = null;
}
