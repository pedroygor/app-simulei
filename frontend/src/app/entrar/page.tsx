"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { CartaoDeAutenticacao } from "@/components/auth/cartao-de-autenticacao";
import { CampoDeTexto } from "@/components/formulario/campo-de-texto";
import { Button } from "@/components/ui/button";
import { ErroDaApi } from "@/lib/api/erros";
import { esquemaDeLogin, type FormularioDeLogin } from "@/lib/validacao/auth";
import { useSessao } from "@/providers/auth-provider";

export default function PaginaDeLogin() {
  const router = useRouter();
  const { entrar } = useSessao();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormularioDeLogin>({
    resolver: zodResolver(esquemaDeLogin),
    defaultValues: { email: "", senha: "" },
  });

  async function aoEnviar(dados: FormularioDeLogin) {
    try {
      await entrar(dados);
      router.replace("/inicio");
    } catch (erro) {
      // Credencial inválida NÃO é erro de campo: o backend devolve a mesma
      // mensagem para e-mail inexistente e senha errada, de propósito —
      // apontar qual dos dois está errado entregaria quais e-mails existem
      const mensagem =
        erro instanceof ErroDaApi ? erro.message : "Não foi possível entrar. Tente de novo.";
      toast.error(mensagem);
    }
  }

  return (
    <CartaoDeAutenticacao
      titulo="Entrar"
      descricao="Acesse seus simulados"
      rodape={
        <>
          Ainda não tem conta?{" "}
          <Link href="/cadastro" className="text-foreground font-medium underline underline-offset-4">
            Criar conta
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(aoEnviar)} className="grid gap-4" noValidate>
        <CampoDeTexto
          rotulo="E-mail"
          type="email"
          autoComplete="email"
          // O aluno abre no celular pra entrar: já foca o primeiro campo
          autoFocus
          placeholder="voce@exemplo.com"
          erro={errors.email?.message}
          {...register("email")}
        />
        <CampoDeTexto
          rotulo="Senha"
          type="password"
          autoComplete="current-password"
          erro={errors.senha?.message}
          {...register("senha")}
        />
        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting ? "Entrando…" : "Entrar"}
        </Button>
      </form>
    </CartaoDeAutenticacao>
  );
}
