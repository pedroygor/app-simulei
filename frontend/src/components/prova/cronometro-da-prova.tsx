"use client";

import { cva } from "class-variance-authority";
import { AlarmClock, Clock } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { avisoFalado, faseDoCronometro, formatarRestante, restanteMs } from "@/lib/prova/cronometro";

const relogio = cva("inline-flex items-center gap-1.5 rounded-md border px-2 py-1 font-mono text-sm tabular-nums", {
  variants: {
    fase: {
      normal: "border-border text-foreground",
      // Nos 5 min finais muda a cor E o ícone E aparece texto: cor sozinha não
      // é sinal (identidade visual)
      acabando: "border-destructive text-destructive font-semibold",
      esgotado: "border-destructive text-destructive font-semibold",
    },
  },
});

/**
 * Quanto falta pro prazo, pelo relógio do SERVIDOR (o `desvio` desconta o do
 * celular). É só exibição: quem recusa resposta atrasada é o backend.
 *
 * `aoEsgotar` é chamado uma vez, quando chega a zero (entrega automática, 4º
 * incremento). Tique de 1 s recalculado do relógio, não decrementado: um
 * contador que subtrai 1 a cada tique atrasa quando a aba fica em segundo plano
 * (o navegador espaça os timers), e o cálculo a partir do prazo não.
 */
export function CronometroDaProva({
  prazo,
  desvio,
  aoEsgotar,
}: {
  prazo: string;
  desvio: number;
  aoEsgotar?: () => void;
}) {
  const [restante, setRestante] = useState(() => restanteMs(prazo, desvio, Date.now()));
  const [aviso, setAviso] = useState<string | null>(null);
  // Refs lidos/escritos só no callback do intervalo (nunca no render)
  const anterior = useRef(restante);
  const jaEsgotou = useRef(false);
  const aoEsgotarAtual = useRef(aoEsgotar);

  useEffect(() => {
    aoEsgotarAtual.current = aoEsgotar;
  }, [aoEsgotar]);

  useEffect(() => {
    function tique() {
      const agora = restanteMs(prazo, desvio, Date.now());
      const falado = avisoFalado(anterior.current, agora);
      if (falado) setAviso(falado);
      anterior.current = agora;
      setRestante(agora);
      if (agora <= 0 && !jaEsgotou.current) {
        jaEsgotou.current = true;
        aoEsgotarAtual.current?.();
      }
    }
    tique();
    const intervalo = window.setInterval(tique, 1000);
    return () => window.clearInterval(intervalo);
  }, [prazo, desvio]);

  const fase = faseDoCronometro(restante);
  const Icone = fase === "normal" ? Clock : AlarmClock;

  return (
    <>
      {/* role="timer": o leitor de tela NÃO anuncia cada mudança (aria-live off
          por padrão). Quem anuncia são os marcos, na região abaixo */}
      <div role="timer" aria-label="Tempo restante" className={relogio({ fase })}>
        <Icone aria-hidden className="size-4" />
        <span>{formatarRestante(restante)}</span>
        {fase === "acabando" && <span className="font-sans text-xs">acabando</span>}
      </div>
      <p aria-live="assertive" className="sr-only">
        {aviso}
      </p>
    </>
  );
}
