import type { HabilidadeDaMatriz as Habilidade } from "@/lib/api/questoes";

/**
 * A habilidade oficial do INEP que a questão avalia, com o texto da Matriz de
 * Referência. Complementa o assunto: o assunto diz SOBRE O QUÊ é a questão
 * ("Probabilidade"); a habilidade diz o que ela pede do aluno, na linguagem que o
 * professor reconhece da matriz.
 *
 * O texto da competência é longo demais pra 360px: fica recolhido num <details>,
 * que já é acessível por teclado e leitor de tela sem JS nenhum.
 *
 * Questão sem habilidade publicada não mostra nada, pelo mesmo motivo da
 * EtiquetaDeAssunto: "sem habilidade" é ruído pra quem lê.
 */
export function HabilidadeDaMatriz({ habilidade }: { habilidade: Habilidade | null }) {
  if (!habilidade) return null;

  const { codigo, descricao, competencia } = habilidade;

  return (
    <section aria-labelledby="habilidade-da-matriz" className="space-y-2 rounded-lg border p-4 text-sm">
      <h2 id="habilidade-da-matriz" className="text-muted-foreground text-xs font-medium">
        Matriz do ENEM · Competência {competencia.numero} · H{codigo}
      </h2>
      <p>{descricao}</p>
      <details className="text-muted-foreground text-xs">
        <summary className="cursor-pointer">O que é a competência {competencia.numero}</summary>
        <p className="mt-1">{competencia.descricao}</p>
      </details>
    </section>
  );
}
