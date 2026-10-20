import type { Desempenho } from "@/lib/api/desempenho";

/**
 * O número geral. A frase diz "das que você respondeu" porque é isso que o
 * percentual mede — e explica por que a em branco fica de fora, pra ninguém
 * confundir com a nota da prova (que conta a em branco como zero, igual ao ENEM).
 */
export function ResumoDoDesempenho({ desempenho }: { desempenho: Desempenho }) {
  const { geral, provas } = desempenho;
  return (
    <section aria-labelledby="no-geral" className="grid gap-1 rounded-lg border p-4">
      <h2 id="no-geral" className="text-muted-foreground text-sm">
        No geral, em {provas === 1 ? "1 prova entregue" : `${provas} provas entregues`}
      </h2>
      {geral.percentual !== null && (
        <p className="font-heading text-4xl font-bold tabular-nums">{geral.percentual}%</p>
      )}
      <p className="text-sm tabular-nums">
        Você acertou {geral.acertos} das {geral.respondidas} questões que respondeu.
      </p>
      {geral.emBranco > 0 && (
        <p className="text-muted-foreground text-sm tabular-nums">
          {geral.emBranco === 1 ? "1 ficou" : `${geral.emBranco} ficaram`} em branco e não
          {geral.emBranco === 1 ? " entra" : " entram"} na conta: em branco pode ser falta de tempo, não de
          conhecimento. No ENEM, errar não tira ponto, então vale responder todas.
        </p>
      )}
    </section>
  );
}
