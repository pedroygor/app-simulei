/**
 * Avisa que questões saíram da prova aberta (o INEP anulou ou a questão foi
 * marcada incompleta). role="status": o leitor de tela anuncia sem interromper.
 */
export function AvisoDeRetirada({ retiradas }: { retiradas: number }) {
  return (
    <p role="status" className="border-contorno rounded-lg border p-3 text-sm">
      {retiradas === 1 ? "1 questão foi retirada" : `${retiradas} questões foram retiradas`} desta prova (anulada
      pelo INEP ou sem o texto necessário). A numeração mudou; suas outras respostas continuam salvas.
    </p>
  );
}
