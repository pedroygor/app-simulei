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
import { areaDaMateria, assuntosDaMateria, buscarTaxonomia, materiasDaArea } from "@/lib/api/taxonomia";
import {
  comOpcaoSelecionada,
  montarOpcoesDeAno,
  montarUrlDeQuestoes,
  mudarFiltro,
  NOME_DA_AREA,
  NOME_DO_IDIOMA,
  temFiltroAtivo,
  type OpcaoDeFiltro,
} from "@/lib/questoes/filtro-da-url";

/**
 * O Select do Radix não representa "sem filtro" com value="", porque string
 * vazia é o jeito dele dizer "nada selecionado". Então usamos um valor
 * sentinela e traduzimos na borda.
 */
const TODOS = "__todos__";

// Anos e taxonomia só mudam com importação ou migration — não há por que
// perguntar de novo a cada troca de filtro ou volta pra tela
const UMA_HORA = 60 * 60 * 1000;

export function FiltrosDeQuestoes({ filtro }: { filtro: FiltroDeQuestoes }) {
  const router = useRouter();

  const { data: anosDisponiveis } = useQuery({
    queryKey: ["questoes", "anos"],
    queryFn: buscarAnosDisponiveis,
    staleTime: UMA_HORA,
  });
  const { data: taxonomia } = useQuery({
    queryKey: ["taxonomia"],
    queryFn: buscarTaxonomia,
    staleTime: UMA_HORA,
  });

  const anos = montarOpcoesDeAno(anosDisponiveis, filtro.anoInicial);
  const materias = comOpcaoSelecionada(
    materiasDaArea(taxonomia, filtro.area).map((m) => ({ valor: m.slug, rotulo: m.nome })),
    filtro.materia,
  );
  const assuntos = comOpcaoSelecionada(
    assuntosDaMateria(taxonomia, filtro.materia).map((a) => ({ valor: a.slug, rotulo: a.nome })),
    filtro.assunto,
  );

  function aplicar(mudanca: Partial<FiltroDeQuestoes>) {
    router.push(montarUrlDeQuestoes(mudarFiltro(filtro, mudanca)));
  }

  // Escolher a matéria já define a área (Física → Natureza). Sem isto, com
  // "Todas as áreas", o filtro de área mentiria sobre o que está na tela
  function aplicarMateria(materia: string | undefined) {
    const area = materia ? areaDaMateria(taxonomia, materia) : filtro.area;
    aplicar({ area, materia });
  }

  return (
    // Mobile-first: uma coluna no celular; a partir de sm, a hierarquia
    // área → matéria → assunto numa linha e idioma/ano na seguinte
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
        id="filtro-materia"
        rotulo="Matéria"
        valor={filtro.materia}
        aoMudar={aplicarMateria}
        opcoes={materias}
        rotuloDeTodos="Todas as matérias"
      />

      <CampoDeSelecao
        id="filtro-assunto"
        rotulo="Assunto"
        valor={filtro.assunto}
        aoMudar={(assunto) => aplicar({ assunto })}
        opcoes={assuntos}
        // Sem matéria não há lista de assuntos; desabilitado e dizendo por quê
        desabilitado={!filtro.materia}
        rotuloDeTodos={filtro.materia ? "Todos os assuntos" : "Escolha a matéria"}
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

      {temFiltroAtivo(filtro) && (
        <div className="sm:col-span-3">
          <Button variant="ghost" size="sm" onClick={() => router.push("/questoes")}>
            Limpar filtros
          </Button>
        </div>
      )}
    </div>
  );
}

function CampoDeSelecao({
  id,
  rotulo,
  valor,
  aoMudar,
  opcoes,
  rotuloDeTodos,
  desabilitado = false,
}: {
  id: string;
  rotulo: string;
  valor: string | undefined;
  aoMudar: (valor: string | undefined) => void;
  opcoes: OpcaoDeFiltro[];
  rotuloDeTodos: string;
  desabilitado?: boolean;
}) {
  return (
    // min-w-0: item de grid não encolhe abaixo do conteúdo por padrão. Sem
    // isto, "Eletrodinâmica: circuitos, potência e consumo elétrico" alargava
    // a coluna e a página ganhava rolagem lateral a 360px (medido: 404px)
    <div className="grid min-w-0 gap-1.5">
      <Label htmlFor={id}>{rotulo}</Label>
      <Select
        value={valor ?? TODOS}
        onValueChange={(novo) => aoMudar(novo === TODOS ? undefined : novo)}
        disabled={desabilitado}
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
