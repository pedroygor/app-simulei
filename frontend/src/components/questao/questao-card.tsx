import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { QuestaoResumo } from "@/lib/api/questoes";
import { NOME_DO_IDIOMA } from "@/lib/questoes/filtro-da-url";
import { EtiquetaDeArea } from "./etiqueta-de-area";

/**
 * `href` vem de fora porque o cartão não sabe de que listagem faz parte — quem
 * sabe é a lista, que monta o link levando os filtros pro "voltar" do detalhe.
 */
export function QuestaoCard({ questao, href }: { questao: QuestaoResumo; href: string }) {
  return (
    // relative ancora o link esticado do ::after abaixo; sem isto ele se
    // estenderia até o ancestral posicionado mais próximo, cobrindo a página
    <Card className="relative transition-colors hover:border-ring">
      <CardContent className="p-4">
        {/*
          O link cobre o cartão inteiro (o ::after esticado) em vez de o cartão
          virar <div onClick>: continua sendo âncora de verdade — foco pelo
          teclado, "abrir em nova aba", e leitor de tela anuncia como link.
        */}
        <Link
          href={href}
          className="after:absolute after:inset-0 focus-visible:outline-none"
        >
          <h2 className="font-heading text-base font-semibold">
            ENEM {questao.ano} · questão {questao.numero}
          </h2>
        </Link>

        <div className="text-muted-foreground mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1">
          <EtiquetaDeArea area={questao.area} />
          {questao.idioma && (
            <Badge variant="outline">{NOME_DO_IDIOMA[questao.idioma]}</Badge>
          )}
          {questao.materia && <span className="text-xs">{questao.materia}</span>}
        </div>

        {questao.previa && (
          /*
            TEXTO PURO, não markdown. A prévia é cortada por substring no banco
            e pode terminar no meio de um `![](...)` — medido: 9 das 182
            questões de 2023. Renderizar sintaxe truncada mostraria lixo.
          */
          <p className="text-muted-foreground mt-2 line-clamp-2 text-sm">{questao.previa}</p>
        )}
      </CardContent>
    </Card>
  );
}
