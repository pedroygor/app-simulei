"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { toast } from "sonner";
import { MolduraDeAcesso } from "@/components/auth/moldura-de-acesso";
import { SeletorDePerfil } from "@/components/auth/seletor-de-perfil";
import { CampoDeSenha } from "@/components/formulario/campo-de-senha";
import { CampoDeTexto } from "@/components/formulario/campo-de-texto";
import { Button } from "@/components/ui/button";
import { ErroDaApi } from "@/lib/api/erros";
import { aplicarErrosDeCampo } from "@/lib/api/erros-de-formulario";
import { esquemaDeCadastro, type FormularioDeCadastro } from "@/lib/validacao/auth";
import { useSessao } from "@/providers/auth-provider";

const CAMPOS_DO_FORMULARIO = ["nome", "email", "senha", "role"] as const;

export default function PaginaDeCadastro() {
  const router = useRouter();
  const { cadastrar, entrar } = useSessao();

  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormularioDeCadastro>({
    resolver: zodResolver(esquemaDeCadastro),
    defaultValues: { nome: "", email: "", senha: "", role: "ALUNO" },
  });

  async function aoEnviar(dados: FormularioDeCadastro) {
    try {
      await cadastrar(dados);
      // Cadastrou, já entra: pedir pro usuário digitar de novo o que acabou
      // de digitar é atrito sem motivo
      await entrar({ email: dados.email, senha: dados.senha });
      router.replace("/inicio");
    } catch (erro) {
      // 400 com campos[] vira erro no campo certo; o resto vira aviso geral
      if (aplicarErrosDeCampo(erro, setError, CAMPOS_DO_FORMULARIO)) {
        return;
      }
      if (erro instanceof ErroDaApi && erro.status === 409) {
        setError("email", { type: "server", message: "Este e-mail já está cadastrado" });
        return;
      }
      toast.error(
        erro instanceof ErroDaApi ? erro.message : "Não foi possível criar a conta. Tente de novo.",
      );
    }
  }

  return (
    <MolduraDeAcesso
      titulo="Criar conta"
      subtitulo="Leva menos de um minuto. Depois é só escolher o simulado."
      ativo="cadastro"
    >
      <form onSubmit={handleSubmit(aoEnviar)} className="flex flex-1 flex-col gap-5" noValidate>
        {/* Controller é o caminho pra componente controlado customizado:
            watch() não é memoizável e o React Compiler avisa sobre isso */}
        <Controller
          control={control}
          name="role"
          render={({ field }) => (
            <SeletorDePerfil valor={field.value} aoMudar={field.onChange} />
          )}
        />
        <CampoDeTexto
          rotulo="Nome completo"
          autoComplete="name"
          // Nome completo: é como o professor identifica o aluno na turma
          placeholder="Seu nome e sobrenome"
          erro={errors.nome?.message}
          {...register("nome")}
        />
        <CampoDeTexto
          rotulo="E-mail"
          type="email"
          inputMode="email"
          autoComplete="email"
          placeholder="voce@escola.com.br"
          erro={errors.email?.message}
          {...register("email")}
        />
        <CampoDeSenha
          rotulo="Senha"
          autoComplete="new-password"
          ajuda="Pelo menos 8 caracteres"
          erro={errors.senha?.message}
          {...register("senha")}
        />
        <div className="mt-auto pt-4">
          <Button type="submit" size="xl" disabled={isSubmitting} className="w-full">
            {isSubmitting ? "Criando conta…" : "Criar minha conta"}
          </Button>
        </div>
      </form>
    </MolduraDeAcesso>
  );
}
