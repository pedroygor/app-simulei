import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

/**
 * Entregar é irreversível (a prova finalizada não aceita resposta), então pede
 * confirmação dizendo o que fica pra trás. AlertDialog e não Dialog: prende o
 * foco e não fecha clicando fora — é decisão, não informação.
 *
 * Com resposta ainda não salva, entregar é bloqueado: o servidor corrigiria uma
 * prova diferente da que o aluno vê na tela.
 */
export function ConfirmarEntrega({
  aberto,
  aoMudarAberto,
  emBranco,
  naoSalvas,
  entregando,
  erro,
  aoConfirmar,
}: {
  aberto: boolean;
  aoMudarAberto: (aberto: boolean) => void;
  emBranco: number;
  naoSalvas: number;
  entregando: boolean;
  erro: string | null;
  aoConfirmar: () => void;
}) {
  return (
    <AlertDialog open={aberto} onOpenChange={aoMudarAberto}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Entregar a prova?</AlertDialogTitle>
          <AlertDialogDescription>
            {emBranco === 0
              ? "Você respondeu todas as questões."
              : `${emBranco} ${emBranco === 1 ? "questão ficou" : "questões ficaram"} em branco.`}{" "}
            Depois de entregar, não dá para mudar as respostas.
          </AlertDialogDescription>
        </AlertDialogHeader>
        {naoSalvas > 0 && (
          <p role="alert" className="text-destructive text-sm">
            {naoSalvas === 1 ? "1 resposta ainda não foi salva" : `${naoSalvas} respostas ainda não foram salvas`}.
            Espere salvar ou toque em &quot;Tentar de novo&quot; na questão.
          </p>
        )}
        {erro && (
          <p role="alert" className="text-destructive text-sm">
            {erro}
          </p>
        )}
        <AlertDialogFooter>
          <AlertDialogCancel>Voltar à prova</AlertDialogCancel>
          <AlertDialogAction
            disabled={naoSalvas > 0 || entregando}
            onClick={(evento) => {
              // O Action fecharia o diálogo sozinho; ele só fecha quando a
              // entrega der certo (a tela troca pra "prova entregue")
              evento.preventDefault();
              aoConfirmar();
            }}
          >
            {entregando ? "Entregando…" : "Entregar"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
