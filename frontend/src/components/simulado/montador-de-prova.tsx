"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Controller, useFieldArray, useForm, useWatch, type Path } from "react-hook-form";
import { CampoDeSelecao } from "@/components/formulario/campo-de-selecao";
import { CampoDeTexto } from "@/components/formulario/campo-de-texto";
import { ErroDaApi } from "@/lib/api/erros";
import { buscarAnosDisponiveis, IDIOMAS, type Idioma } from "@/lib/api/questoes";
import { criarSimulado, type SimuladoCriado } from "@/lib/api/simulados";
import { buscarTaxonomia } from "@/lib/api/taxonomia";
import { NOME_DO_IDIOMA } from "@/lib/questoes/filtro-da-url";
import {
  campoDoFormulario,
  montarPedido,
  precisaDeIdioma,
  problemasDaMontagem,
  questoesDaEscolha,
} from "@/lib/simulados/montagem";
import {
  esquemaDeMontagem,
  numeroOuVazio,
  type EntradaDaMontagem,
  type FormularioValidado,
} from "@/lib/validacao/simulado";
import { AdicionarEscolha } from "./adicionar-escolha";
import { BarraDeEnvio } from "./barra-de-envio";
import { ListaDeEscolhas } from "./lista-de-escolhas";
import { ResultadoDaMontagem } from "./resultado-da-montagem";
import { SeletorDeAnos } from "./seletor-de-anos";

// Anos e taxonomia só mudam com importação ou migration
const UMA_HORA = 60 * 60 * 1000;

const VALORES_INICIAIS: EntradaDaMontagem = { titulo: "", total: 10, anos: [], itens: [] };

// Erros de campo do backend que têm um campo na tela; o resto vira aviso geral
const CAMPO_DA_TELA = /^(titulo|total|tempoLimiteMin|itens\.\d+\.quantidade)$/;

/**
 * Montar prova (Etapa 3a): total, anos, escolhas de área/matéria/assunto com
 * quantidade opcional, idioma quando Linguagens inteira entra. As regras vivem em
 * lib/simulados/montagem.ts (testadas); aqui só a fiação da tela.
 */
export function MontadorDeProva() {
  const [criado, setCriado] = useState<SimuladoCriado | null>(null);
  const [erroGeral, setErroGeral] = useState<string | null>(null);

  const {
    control,
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<EntradaDaMontagem, unknown, FormularioValidado>({
    resolver: zodResolver(esquemaDeMontagem),
    defaultValues: VALORES_INICIAIS,
  });
  // useFieldArray: lista dinâmica no react-hook-form; cada item ganha um `id`
  // estável, que vira a key — o índice muda quando um item sai do meio
  const { fields, append, remove } = useFieldArray({ control, name: "itens" });
  // useWatch e não watch(): o watch() não é memoizável e o React Compiler o reprova
  const [anos, itens, total, idioma] = useWatch({ control, name: ["anos", "itens", "total", "idioma"] });

  const { data: anosDisponiveis = [] } = useQuery({
    queryKey: ["questoes", "anos"],
    queryFn: buscarAnosDisponiveis,
    staleTime: UMA_HORA,
  });
  const anosDaConsulta = [...anos].sort((a, b) => a - b);
  const { data: taxonomia } = useQuery({
    // Os anos fazem parte da chave: cada combinação é uma contagem diferente
    queryKey: ["taxonomia", anosDaConsulta],
    queryFn: () => buscarTaxonomia(anosDaConsulta),
    staleTime: UMA_HORA,
    // Mantém os números antigos na tela enquanto os novos chegam, sem piscar
    placeholderData: keepPreviousData,
  });

  const montagem = useMutation({
    mutationFn: criarSimulado,
    onSuccess: (simulado) => setCriado(simulado),
    onError: (erro) => {
      if (erro instanceof ErroDaApi && erro.temErrosDeCampo) {
        const semCampo: string[] = [];
        for (const { campo, mensagem } of erro.campos) {
          const nome = campoDoFormulario(campo);
          if (CAMPO_DA_TELA.test(nome)) setError(nome as Path<EntradaDaMontagem>, { type: "server", message: mensagem });
          else semCampo.push(mensagem);
        }
        setErroGeral(semCampo.length > 0 ? semCampo.join(". ") : null);
        return;
      }
      // 422 (sobreposição, falta de questões) traz a explicação pronta do backend
      setErroGeral(erro instanceof ErroDaApi ? erro.message : "Não foi possível montar a prova. Tente de novo.");
    },
  });

  function aoEnviar(formulario: FormularioValidado) {
    setErroGeral(null);
    montagem.mutate(montarPedido(formulario));
  }

  function montarOutra() {
    reset(VALORES_INICIAIS);
    setCriado(null);
    setErroGeral(null);
  }

  if (criado) return <ResultadoDaMontagem simulado={criado} aoMontarOutra={montarOutra} />;

  const idiomaAtual = precisaDeIdioma(itens) ? (idioma as Idioma | undefined) : undefined;
  const avisos = problemasDaMontagem(Number(total), itens);

  return (
    <form onSubmit={handleSubmit(aoEnviar)} noValidate className="grid gap-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <CampoDeTexto rotulo="Nome da prova" placeholder="Ex.: Revisão de Natureza" erro={errors.titulo?.message} {...register("titulo")} />
        <CampoDeTexto
          rotulo="Questões"
          type="number"
          inputMode="numeric"
          min={5}
          max={180}
          erro={errors.total?.message}
          {...register("total", { setValueAs: numeroOuVazio })}
        />
      </div>

      <Controller
        control={control}
        name="anos"
        render={({ field }) => (
          <SeletorDeAnos disponiveis={anosDisponiveis} selecionados={field.value} aoMudar={field.onChange} />
        )}
      />

      <section aria-labelledby="escolhas" className="grid gap-3">
        <h2 id="escolhas" className="text-sm font-medium">
          O que cai na prova
        </h2>
        <AdicionarEscolha
          taxonomia={taxonomia}
          escolhas={itens}
          idioma={idiomaAtual}
          aoAdicionar={(escolha) => append(escolha)}
        />
        <ListaDeEscolhas
          aoRemover={remove}
          itens={fields.map((campo, indice) => ({
            chave: campo.id,
            rotulo: campo.rotulo,
            questoes: taxonomia ? questoesDaEscolha(taxonomia, campo, idiomaAtual) : 0,
            campoDeQuantidade: register(`itens.${indice}.quantidade`, { setValueAs: numeroOuVazio }),
            erro: errors.itens?.[indice]?.quantidade?.message,
          }))}
        />
        {errors.itens?.message && (
          <p role="alert" className="text-destructive text-sm">
            {errors.itens.message}
          </p>
        )}
      </section>

      {/* O idioma só muda algo com Linguagens inteira na prova */}
      {precisaDeIdioma(itens) && (
        <Controller
          control={control}
          name="idioma"
          render={({ field }) => (
            <CampoDeSelecao
              id="montagem-idioma"
              rotulo="Língua estrangeira"
              valor={field.value}
              aoMudar={field.onChange}
              opcoes={IDIOMAS.map((i) => ({ valor: i, rotulo: NOME_DO_IDIOMA[i] }))}
              rotuloDeTodos="Sem língua estrangeira"
            />
          )}
        />
      )}

      <CampoDeTexto
        rotulo="Tempo (minutos)"
        type="number"
        inputMode="numeric"
        min={1}
        max={360}
        ajuda="Vazio: sem cronômetro"
        erro={errors.tempoLimiteMin?.message}
        {...register("tempoLimiteMin", { setValueAs: numeroOuVazio })}
      />

      {erroGeral && (
        <p role="alert" className="border-destructive/40 text-destructive rounded-lg border p-3 text-sm">
          {erroGeral}
        </p>
      )}

      <BarraDeEnvio total={Number(total)} escolhas={itens.length} avisos={avisos} enviando={montagem.isPending} />
    </form>
  );
}
