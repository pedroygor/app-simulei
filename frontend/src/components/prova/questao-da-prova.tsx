"use client";

import { useEffect, useRef } from "react";
import { EtiquetaDeArea } from "@/components/questao/etiqueta-de-area";
import { TextoDaQuestao } from "@/components/questao/texto-da-questao";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { QuestaoDaProva as Questao } from "@/lib/api/tentativas";
import type { SituacaoDoSalvamento } from "@/lib/prova/fila-de-salvamento";
import { NOME_DO_IDIOMA } from "@/lib/questoes/filtro-da-url";
import { GrupoDeAlternativas } from "./grupo-de-alternativas";
import { SituacaoDaResposta } from "./situacao-da-resposta";

/**
 * Uma questão da prova. Montada de novo a cada troca de questão (key no pai), e
 * por isso o foco vai pro título ao montar: quem usa leitor de tela ouve "Questão
 * 4 de 10" em vez de ficar parado no botão "Próxima", e a página volta ao topo.
 */
export function QuestaoDaProva({
  questao,
  total,
  marcada,
  situacao,
  aoMarcar,
  aoTentarDeNovo,
}: {
  questao: Questao;
  total: number;
  marcada: number | null;
  situacao: SituacaoDoSalvamento;
  aoMarcar: (alternativa: number | null) => void;
  aoTentarDeNovo: () => void;
}) {
  const titulo = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    titulo.current?.focus();
  }, []);

  const idDoTitulo = `titulo-${questao.id}`;
  const idDoComando = `comando-${questao.id}`;

  return (
    <article className="grid gap-5">
      <header className="grid gap-2">
        {/* tabIndex -1: focável por código, mas fora da ordem do Tab */}
        <h2 id={idDoTitulo} ref={titulo} tabIndex={-1} className="font-heading text-xl font-bold outline-none">
          Questão {questao.ordem} <span className="text-muted-foreground font-normal">de {total}</span>
        </h2>
        <div className="text-muted-foreground flex flex-wrap items-center gap-x-3 gap-y-1 text-xs">
          <EtiquetaDeArea area={questao.area} />
          <span>
            ENEM {questao.ano} · questão {questao.numero}
          </span>
          {questao.idioma && <Badge variant="outline">{NOME_DO_IDIOMA[questao.idioma]}</Badge>}
        </div>
      </header>

      <TextoDaQuestao texto={questao.enunciado} />
      {/* O comando é a pergunta: vira o nome do radiogroup pro leitor de tela.
          As 2 questões sem comando usam o título ("Questão 4 de 10") */}
      {questao.comando && (
        <div id={idDoComando}>
          <TextoDaQuestao texto={questao.comando} className="font-medium" />
        </div>
      )}

      <GrupoDeAlternativas
        alternativas={questao.alternativas}
        marcada={marcada}
        aoMarcar={aoMarcar}
        rotuladoPor={questao.comando ? idDoComando : idDoTitulo}
      />

      <div className="flex flex-wrap items-start justify-between gap-2">
        <SituacaoDaResposta situacao={situacao} respondida={marcada !== null} aoTentarDeNovo={aoTentarDeNovo} />
        {/* Radio não se desmarca tocando de novo (nem no nativo): deixar em
            branco é uma ação explícita */}
        {marcada !== null && (
          <Button type="button" variant="ghost" size="sm" onClick={() => aoMarcar(null)}>
            Limpar resposta
          </Button>
        )}
      </div>
    </article>
  );
}
