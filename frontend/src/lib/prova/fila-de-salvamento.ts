/** Situação de uma questão do ponto de vista do servidor. */
export type SituacaoDoSalvamento = "salva" | "salvando" | "erro";

export type Retrato = {
  /** questão → alternativa marcada; null = em branco. Inclui o que ainda não foi salvo. */
  marcadas: ReadonlyMap<number, number | null>;
  /** Só as questões que NÃO estão salvas; ausente = salva. */
  situacoes: ReadonlyMap<number, Exclude<SituacaoDoSalvamento, "salva">>;
};

/** null = desmarcar. */
export type Envio = (questao: number, alternativa: number | null) => Promise<void>;

/**
 * Salva as respostas da prova sem deixar a tela e o servidor discordarem.
 *
 * O problema: o aluno toca B, C e D em sequência (ou segura a seta). Um PUT por
 * toque, em paralelo, pode chegar fora de ordem — o servidor guardaria C com a
 * tela mostrando D. Aqui há NO MÁXIMO UM envio por questão em voo; quando ele
 * volta e o valor desejado mudou nesse meio-tempo, sai um novo envio só com o
 * ÚLTIMO valor. Três toques rápidos = dois PUTs, sempre na ordem.
 *
 * A tela mostra o desejado na hora (otimista). Falha não desfaz a marcação — no
 * wi-fi ruim do cursinho, apagar a escolha do aluno seria pior — e a questão
 * fica em "erro" até ele tentar de novo.
 *
 * Fora do React de propósito (assinar/retrato no formato do useSyncExternalStore):
 * dá pra testar a ordem dos envios sem renderizar nada.
 */
export class FilaDeSalvamento {
  private readonly desejado: Map<number, number | null>;
  private readonly emVoo = new Set<number>();
  private readonly falhas = new Set<number>();
  private readonly ouvintes = new Set<() => void>();
  private atual: Retrato;

  constructor(
    inicial: ReadonlyMap<number, number | null>,
    private readonly envio: Envio,
    private readonly aoFalhar: (erro: unknown) => void = () => {},
  ) {
    this.desejado = new Map(inicial);
    this.atual = this.fotografar();
  }

  // Arrow functions: o useSyncExternalStore as chama soltas, sem o `this`
  assinar = (ouvinte: () => void) => {
    this.ouvintes.add(ouvinte);
    return () => {
      this.ouvintes.delete(ouvinte);
    };
  };

  retrato = () => this.atual;

  marcar(questao: number, alternativa: number | null) {
    const igual = (this.desejado.get(questao) ?? null) === alternativa;
    // Repetir o que já está salvo não gera requisição; repetir depois de uma
    // falha é justamente o "tentar de novo"
    if (igual && !this.falhas.has(questao)) return;
    this.desejado.set(questao, alternativa);
    this.falhas.delete(questao);
    if (!this.emVoo.has(questao)) {
      void this.enviar(questao);
    } else {
      // O envio em voo, ao voltar, vê que o desejado mudou e manda o novo
      this.publicar();
    }
  }

  tentarDeNovo(questao: number) {
    this.marcar(questao, this.desejado.get(questao) ?? null);
  }

  /**
   * Resolve quando não há mais envio EM VOO, ou quando `limiteMs` passa — o que
   * vier primeiro. É o que a entrega automática espera quando o cronômetro zera:
   * a resposta tocada no último segundo ainda está a caminho, e entregar antes
   * dela a perderia. Falha não conta como em voo: com o tempo acabado, ninguém
   * vai tentar de novo. O limite fica dentro da folga que o servidor dá (10 s).
   */
  aguardarEnvios(limiteMs: number): Promise<void> {
    if (this.emVoo.size === 0) return Promise.resolve();
    return new Promise((resolver) => {
      const prazo = setTimeout(acabar, limiteMs);
      const desassinar = this.assinar(() => {
        if (this.emVoo.size === 0) acabar();
      });
      function acabar() {
        clearTimeout(prazo);
        desassinar();
        resolver();
      }
    });
  }

  /** Quantas questões ainda não estão garantidas no servidor. */
  pendentes() {
    return this.emVoo.size + this.falhas.size;
  }

  private async enviar(questao: number) {
    const valor = this.desejado.get(questao) ?? null;
    this.emVoo.add(questao);
    this.publicar();
    try {
      await this.envio(questao, valor);
    } catch (erro) {
      this.emVoo.delete(questao);
      this.falhas.add(questao);
      this.publicar();
      this.aoFalhar(erro);
      return;
    }
    this.emVoo.delete(questao);
    if ((this.desejado.get(questao) ?? null) !== valor) {
      void this.enviar(questao);
      return;
    }
    this.publicar();
  }

  private publicar() {
    // Objeto NOVO a cada mudança: o useSyncExternalStore compara por referência
    this.atual = this.fotografar();
    for (const ouvinte of this.ouvintes) ouvinte();
  }

  private fotografar(): Retrato {
    const situacoes = new Map<number, "salvando" | "erro">();
    for (const q of this.emVoo) situacoes.set(q, "salvando");
    for (const q of this.falhas) situacoes.set(q, "erro");
    return { marcadas: new Map(this.desejado), situacoes };
  }
}
