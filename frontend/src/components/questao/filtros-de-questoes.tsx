"use client";

import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  AREAS,
  buscarAnosDisponiveis,
  IDIOMAS,
  type Area,
  type FiltroDeQuestoes,
  type Idioma,
} from "@/lib/api/questoes";
import {
  montarOpcoesDeAno,
  montarUrlDeQuestoes,
  NOME_DA_AREA,
  NOME_DO_IDIOMA,
} from "@/lib/questoes/filtro-da-url";

/**
 * O Select do Radix não representa "sem filtro" com value="", porque string
 * vazia é o jeito dele dizer "nada selecionado". Então usamos um valor
 * sentinela e traduzimos na borda.
 */
const TODOS = "__todos__";

export function FiltrosDeQuestoes({ filtro }: { filtro: FiltroDeQuestoes }) {
  const router = useRouter();

  const { data: anosDisponiveis } = useQuery({
    queryKey: ["questoes", "anos"],
    queryFn: buscarAnosDisponiveis,
    // Os anos só mudam quando alguém roda a importação — não há por que
    // perguntar de novo a cada troca de filtro ou volta pra tela
    staleTime: 60 * 60 * 1000,
  });
  const anos = montarOpcoesDeAno(anosDisponiveis, filtro.anoInicial);

  /**
   * Toda mudança de filtro volta pra página 1. Sem isto, quem está na página 7
   * de Matemática e troca pra Linguagens cai numa página 7 que provavelmente
   * não existe — e vê lista vazia achando que não há questões.
   */
  function aplicar(mudanca: Partial<FiltroDeQuestoes>) {
    router.push(montarUrlDeQuestoes({ ...filtro, ...mudanca, pagina: 0 }));
  }

  const temFiltro =
    filtro.area || filtro.idioma || filtro.anoInicial !== undefined || filtro.anoFinal !== undefined;

  return (
    // Mobile-first: uma coluna no celular, três a partir de sm
    <div className="grid gap-3 sm:grid-cols-3">
      <CampoDeSelecao
        id="filtro-area"
        rotulo="Área"
        valor={filtro.area}
        aoMudar={(area) => aplicar({ area: area as Area | undefined })}
        opcoes={AREAS.map((a) => ({ valor: a, rotulo: NOME_DA_AREA[a] }))}
        rotuloDeTodos="Todas as áreas"
      />

      <CampoDeSelecao
        id="filtro-idioma"
        rotulo="Idioma"
        valor={filtro.idioma}
        aoMudar={(idioma) => aplicar({ idioma: idioma as Idioma | undefined })}
        opcoes={IDIOMAS.map((i) => ({ valor: i, rotulo: NOME_DO_IDIOMA[i] }))}
        rotuloDeTodos="Qualquer idioma"
      />

      <CampoDeSelecao
        id="filtro-ano"
        rotulo="Ano"
        valor={filtro.anoInicial !== undefined ? String(filtro.anoInicial) : undefined}
        // Um ano só: inicial e final iguais. O backend aceita intervalo, mas
        // intervalo no celular pede dois seletores e não é o que o professor
        // faz na prática ("quero as de 2023").
        aoMudar={(ano) =>
          aplicar(
            ano === undefined
              ? { anoInicial: undefined, anoFinal: undefined }
              : { anoInicial: Number(ano), anoFinal: Number(ano) },
          )
        }
        opcoes={anos.map((a) => ({ valor: String(a), rotulo: String(a) }))}
        rotuloDeTodos="Todos os anos"
      />

      {temFiltro && (
        <div className="sm:col-span-3">
          <Button variant="ghost" size="sm" onClick={() => router.push("/questoes")}>
            Limpar filtros
          </Button>
        </div>
      )}
    </div>
  );
}

type OpcaoDeFiltro = { valor: string; rotulo: string };

function CampoDeSelecao({
  id,
  rotulo,
  valor,
  aoMudar,
  opcoes,
  rotuloDeTodos,
}: {
  id: string;
  rotulo: string;
  valor: string | undefined;
  aoMudar: (valor: string | undefined) => void;
  opcoes: OpcaoDeFiltro[];
  rotuloDeTodos: string;
}) {
  return (
    <div className="grid gap-1.5">
      <Label htmlFor={id}>{rotulo}</Label>
      <Select
        value={valor ?? TODOS}
        onValueChange={(novo) => aoMudar(novo === TODOS ? undefined : novo)}
      >
        <SelectTrigger id={id} className="w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={TODOS}>{rotuloDeTodos}</SelectItem>
          {opcoes.map((opcao) => (
            <SelectItem key={opcao.valor} value={opcao.valor}>
              {opcao.rotulo}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
