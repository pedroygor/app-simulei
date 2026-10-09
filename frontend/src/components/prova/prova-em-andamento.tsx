"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, useSyncExternalStore } from "react";
import { Button } from "@/components/ui/button";
import { ErroDaApi } from "@/lib/api/erros";
import { desmarcar, finalizarTentativa, responder, type Prova } from "@/lib/api/tentativas";
import { FilaDeSalvamento } from "@/lib/prova/fila-de-salvamento";
import { contarEmBranco, indiceInicial, marcadasIniciais } from "@/lib/prova/prova";
import { AvisoDeRetirada } from "./aviso-de-retirada";
import { ConfirmarEntrega } from "./confirmar-entrega";
import { CronometroDaProva } from "./cronometro-da-prova";
import { MapaDaProva } from "./mapa-da-prova";
import { NavegacaoDaProva } from "./navegacao-da-prova";
import { QuestaoDaProva } from "./questao-da-prova";

/**
 * A prova sendo respondida. Uma questão por tela (densidade de aluno, ver
 * identidade visual), grade pra pular, respostas salvas ao tocar.
 *
 * As marcações vivem na FilaDeSalvamento, não no cache do TanStack: ela é quem
 * sabe o que já chegou ao servidor e ordena os envios. O cache guarda só a prova
 * como veio do servidor.
 */
export function ProvaEmAndamento({
  prova,
  chaveDaConsulta,
  retiradas,
  desvio,
}: {
  prova: Prova;
  chaveDaConsulta: readonly unknown[];
  /** Questões que saíram da prova enquanto ela estava aberta (anuladas no meio). */
  retiradas: number;
  /** Quanto o relógio do servidor está à frente do celular, em ms (ver cronometro.ts). */
  desvio: number;
}) {
  const queryClient = useQueryClient();

  // useState com inicializador: a fila nasce UMA vez, com o que o servidor tinha
  const [fila] = useState(
    () =>
      new FilaDeSalvamento(
        marcadasIniciais(prova.questoes),
        (questao, alternativa) =>
          alternativa === null
            ? desmarcar(prova.tentativaId, questao)
            : responder(prova.tentativaId, questao, alternativa),
        (erro) => {
          // Prova entregue em outra aba (422) ou que deixou de existir (404):
          // recarregar mostra a situação real, em vez de insistir em salvar
          if (erro instanceof ErroDaApi && (erro.status === 422 || erro.status === 404)) {
            void queryClient.invalidateQueries({ queryKey: chaveDaConsulta });
          }
        },
      ),
  );
  // useSyncExternalStore: o jeito do React ler um estado que vive FORA dele (a
  // fila) e re-renderizar quando ele muda, sem leitura "rasgada" no meio
  const { marcadas, situacoes } = useSyncExternalStore(fila.assinar, fila.retrato, fila.retrato);

  const [indice, setIndice] = useState(() => indiceInicial(prova.questoes));
  const [mapaAberto, setMapaAberto] = useState(false);
  const [confirmando, setConfirmando] = useState(false);

  // Fechar a aba com resposta em voo perderia a resposta: o navegador pergunta
  useEffect(() => {
    function avisar(evento: BeforeUnloadEvent) {
      if (fila.pendentes() > 0) evento.preventDefault();
    }
    window.addEventListener("beforeunload", avisar);
    return () => window.removeEventListener("beforeunload", avisar);
  }, [fila]);

  const entrega = useMutation({
    mutationFn: () => finalizarTentativa(prova.tentativaId),
    onSuccess: (tentativa) => {
      // A tela troca pra "entregue" com as marcações que o servidor tem — que,
      // com a entrega bloqueada enquanto há pendência, são as da tela
      queryClient.setQueryData<Prova>(chaveDaConsulta, {
        ...prova,
        status: tentativa.status,
        finalizadoEm: tentativa.finalizadoEm,
        questoes: prova.questoes.map((q) => ({ ...q, marcada: marcadas.get(q.id) ?? null })),
      });
    },
    onError: (erro) => {
      // Já entregue (outra aba): não é falha, é só recarregar
      if (erro instanceof ErroDaApi && erro.status === 422) {
        void queryClient.invalidateQueries({ queryKey: chaveDaConsulta });
      }
    },
  });

  const questao = prova.questoes[indice];
  const total = prova.questoes.length;
  const emBranco = contarEmBranco(prova.questoes, marcadas);

  function irPara(destino: number) {
    setIndice(destino);
    setMapaAberto(false);
  }

  function abrirEntrega() {
    entrega.reset();
    setConfirmando(true);
  }

  return (
    <div className="grid gap-6">
      <header className="grid gap-3">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="font-heading truncate text-lg font-bold">{prova.titulo}</h1>
            <p className="text-muted-foreground text-sm tabular-nums">
              {total - emBranco} de {total} respondidas
            </p>
            {/* Só com tempo-limite; a entrega automática no zero é o próximo passo */}
            {prova.prazo && (
              <div className="mt-2">
                <CronometroDaProva prazo={prova.prazo} desvio={desvio} />
              </div>
            )}
          </div>
          <Button
            type="button"
            variant="outline"
            aria-expanded={mapaAberto}
            aria-controls="mapa-da-prova"
            onClick={() => setMapaAberto((aberto) => !aberto)}
          >
            {mapaAberto ? "Fechar" : "Questões"}
          </Button>
        </div>

        {mapaAberto && (
          <div id="mapa-da-prova" className="grid gap-3 rounded-lg border p-3">
            <MapaDaProva
              atual={indice}
              aoIr={irPara}
              marcadores={prova.questoes.map((q) => ({
                ordem: q.ordem,
                respondida: (marcadas.get(q.id) ?? null) !== null,
                situacao: situacoes.get(q.id) ?? "salva",
              }))}
            />
            {/* outline: com a bolha atual e o "Próxima" já em marca-texto, um
                terceiro bloco amarelo competiria pela atenção */}
            <Button type="button" variant="outline" onClick={abrirEntrega}>
              Entregar prova
            </Button>
          </div>
        )}
      </header>

      {retiradas > 0 && <AvisoDeRetirada retiradas={retiradas} />}

      {/* key: cada questão monta de novo — foco no título e topo da página */}
      <QuestaoDaProva
        key={questao.id}
        questao={questao}
        total={total}
        marcada={marcadas.get(questao.id) ?? null}
        situacao={situacoes.get(questao.id) ?? "salva"}
        aoMarcar={(alternativa) => fila.marcar(questao.id, alternativa)}
        aoTentarDeNovo={() => fila.tentarDeNovo(questao.id)}
      />

      <NavegacaoDaProva indice={indice} total={total} aoIr={irPara} aoEntregar={abrirEntrega} />

      <ConfirmarEntrega
        aberto={confirmando}
        aoMudarAberto={setConfirmando}
        emBranco={emBranco}
        naoSalvas={situacoes.size}
        entregando={entrega.isPending}
        erro={entrega.error && !(entrega.error instanceof ErroDaApi && entrega.error.status === 422) ? entrega.error.message : null}
        aoConfirmar={() => entrega.mutate()}
      />
    </div>
  );
}
