/** Formato de erro padronizado da API (ver GlobalExceptionHandler no backend). */
export type CampoInvalido = {
  campo: string;
  mensagem: string;
};

export type CorpoDeErro = {
  timestamp: string;
  status: number;
  erro: string;
  mensagem: string;
  path: string;
  campos?: CampoInvalido[];
};

/**
 * Erro da API como EXCEÇÃO, não como valor de retorno.
 *
 * Sem isso, toda função de API devolveria algo como `{ dados?, erro? }` e cada
 * chamador teria que lembrar de checar — quem esquecer, segue com dado
 * indefinido. Lançando, quem não trata quebra alto e cedo.
 */
export class ErroDaApi extends Error {
  readonly status: number;
  readonly campos: CampoInvalido[];

  constructor(status: number, mensagem: string, campos: CampoInvalido[] = []) {
    super(mensagem);
    this.name = "ErroDaApi";
    this.status = status;
    this.campos = campos;
  }

  /** 400 com lista de campos: dá pra devolver o erro pro campo certo do form. */
  get temErrosDeCampo() {
    return this.campos.length > 0;
  }
}

/** Falha de rede: a requisição nem chegou (API fora do ar, DNS, offline). */
export class ErroDeRede extends Error {
  constructor(causa: unknown) {
    super("Não foi possível falar com o servidor. Verifique sua conexão.");
    this.name = "ErroDeRede";
    this.cause = causa;
  }
}
