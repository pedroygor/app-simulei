"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Controller, useForm } from "react-hook-form";
import { toast } from "sonner";
import { CartaoDeAutenticacao } from "@/components/auth/cartao-de-autenticacao";
import { SeletorDePerfil } from "@/components/auth/seletor-de-perfil";
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
    <CartaoDeAutenticacao
      titulo="Criar conta"
      descricao="Comece a treinar para o ENEM"
      rodape={
        <>
          Já tem conta?{" "}
          <Link href="/entrar" className="text-foreground font-medium underline underline-offset-4">
            Entrar
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(aoEnviar)} className="grid gap-4" noValidate>
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
          rotulo="Nome"
          autoComplete="name"
          placeholder="Como você quer ser chamado"
          erro={errors.nome?.message}
          {...register("nome")}
        />
        <CampoDeTexto
          rotulo="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@exemplo.com"
          erro={errors.email?.message}
          {...register("email")}
        />
        <CampoDeTexto
          rotulo="Senha"
          type="password"
          autoComplete="new-password"
          ajuda="Pelo menos 8 caracteres"
          erro={errors.senha?.message}
          {...register("senha")}
        />
        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting ? "Criando conta…" : "Criar conta"}
        </Button>
      </form>
    </CartaoDeAutenticacao>
  );
}
