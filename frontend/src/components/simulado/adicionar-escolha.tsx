"use client";

import { Plus } from "lucide-react";
import { useState } from "react";
import { CampoDeSelecao } from "@/components/formulario/campo-de-selecao";
import { Button } from "@/components/ui/button";
import { AREAS, type Area, type Idioma } from "@/lib/api/questoes";
import { areaDaMateria, assuntosDaMateria, materiasDaArea, type Taxonomia } from "@/lib/api/taxonomia";
import { NOME_DA_AREA } from "@/lib/questoes/filtro-da-url";
import {
  escolhaDaCascata,
  motivoParaRecusar,
  questoesDaEscolha,
  type EscolhaNaTela,
} from "@/lib/simulados/montagem";

/**
 * Cascata área → matéria → assunto pra montar UMA escolha. A escolha é o nível
 * mais fundo selecionado: só a área = a área inteira; área + matéria = a
 * matéria; até o assunto = só o assunto.
 *
 * Cada opção mostra quantas questões há NOS ANOS ESCOLHIDOS (a taxonomia chega
 * já filtrada por ano): "Probabilidade (31)" é o que o sorteio pode usar.
 */
export function AdicionarEscolha({
  taxonomia,
  escolhas,
  idioma,
  aoAdicionar,
}: {
  taxonomia: Taxonomia | undefined;
  escolhas: EscolhaNaTela[];
  idioma: Idioma | undefined;
  aoAdicionar: (escolha: EscolhaNaTela) => void;
}) {
  const [area, setArea] = useState<Area | undefined>();
  const [materia, setMateria] = useState<string | undefined>();
  const [assunto, setAssunto] = useState<string | undefined>();
  const [recusa, setRecusa] = useState<string | null>(null);

  const comContagem = (rotulo: string, questoes: number) => `${rotulo} (${questoes})`;
  const contar = (tipo: EscolhaNaTela["tipo"], valor: string) =>
    taxonomia ? questoesDaEscolha(taxonomia, { tipo, valor }, idioma) : 0;

  const alvo = escolhaDaCascata({ area, materia, assunto });
  const rotuloDoAlvo = alvo && taxonomia ? rotulo(taxonomia, alvo.tipo, alvo.valor) : undefined;
  const questoesDoAlvo = alvo ? contar(alvo.tipo, alvo.valor) : 0;

  function adicionar() {
    if (!alvo || !taxonomia || !rotuloDoAlvo) return;
    const nova: EscolhaNaTela = { ...alvo, rotulo: rotuloDoAlvo };
    const motivo = motivoParaRecusar(taxonomia, escolhas, nova, idioma);
    setRecusa(motivo);
    if (motivo) return;
    aoAdicionar(nova);
    // Fica na mesma matéria: o caso comum é somar vários assuntos dela
    setAssunto(undefined);
  }

  return (
    <div className="grid gap-3">
      <CampoDeSelecao
        id="montagem-area"
        rotulo="Área"
        valor={area}
        aoMudar={(nova) => {
          setArea(nova as Area | undefined);
          setMateria(undefined);
          setAssunto(undefined);
          setRecusa(null);
        }}
        opcoes={AREAS.map((a) => ({ valor: a, rotulo: comContagem(NOME_DA_AREA[a], contar("AREA", a)) }))}
        rotuloDeTodos="Escolha a área"
      />
      <CampoDeSelecao
        id="montagem-materia"
        rotulo="Matéria"
        valor={materia}
        aoMudar={(nova) => {
          // Escolher a matéria já define a área (Física → Natureza)
          if (nova) setArea(areaDaMateria(taxonomia, nova));
          setMateria(nova);
          setAssunto(undefined);
          setRecusa(null);
        }}
        opcoes={materiasDaArea(taxonomia, area).map((m) => ({
          valor: m.slug,
          rotulo: comContagem(m.nome, contar("MATERIA", m.slug)),
        }))}
        rotuloDeTodos={area ? "A área inteira" : "Todas as matérias"}
      />
      <CampoDeSelecao
        id="montagem-assunto"
        rotulo="Assunto"
        valor={assunto}
        aoMudar={(novo) => {
          setAssunto(novo);
          setRecusa(null);
        }}
        opcoes={assuntosDaMateria(taxonomia, materia).map((a) => ({ valor: a.slug, rotulo: comContagem(a.nome, a.questoes) }))}
        desabilitado={!materia}
        rotuloDeTodos={materia ? "A matéria inteira" : "Escolha a matéria"}
      />

      <Button type="button" variant="outline" disabled={!alvo || questoesDoAlvo === 0} onClick={adicionar}>
        <Plus aria-hidden />
        {/* O botão diz o que vai entrar: "a área inteira" ou só o assunto */}
        {rotuloDoAlvo ? `Adicionar ${rotuloDoAlvo} (${questoesDoAlvo})` : "Adicionar à prova"}
      </Button>
      {recusa && (
        <p role="alert" className="text-destructive text-sm">
          {recusa}
        </p>
      )}
    </div>
  );
}

function rotulo(taxonomia: Taxonomia, tipo: EscolhaNaTela["tipo"], valor: string): string | undefined {
  if (tipo === "AREA") return NOME_DA_AREA[valor as Area];
  const materias = taxonomia.flatMap((a) => a.materias);
  if (tipo === "MATERIA") return materias.find((m) => m.slug === valor)?.nome;
  return materias.flatMap((m) => m.assuntos).find((a) => a.slug === valor)?.nome;
}
