"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { MolduraDeAcesso } from "@/components/auth/moldura-de-acesso";
import { CampoDeSenha } from "@/components/formulario/campo-de-senha";
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
    <MolduraDeAcesso
      titulo="Bom te ver de volta"
      subtitulo="Entre para continuar de onde você parou."
      ativo="entrar"
    >
      {/* flex-1 + mt-auto no botão: no celular o formulário ocupa o resto da
          tela e o botão principal desce pra perto do polegar */}
      <form onSubmit={handleSubmit(aoEnviar)} className="flex flex-1 flex-col gap-5" noValidate>
        <CampoDeTexto
          rotulo="E-mail"
          type="email"
          inputMode="email"
          autoComplete="email"
          // O aluno abre no celular pra entrar: já foca o primeiro campo
          autoFocus
          placeholder="voce@escola.com.br"
          erro={errors.email?.message}
          {...register("email")}
        />
        <CampoDeSenha
          rotulo="Senha"
          autoComplete="current-password"
          erro={errors.senha?.message}
          {...register("senha")}
        />
        <div className="mt-auto pt-4">
          <Button type="submit" size="xl" disabled={isSubmitting} className="w-full">
            {isSubmitting ? "Entrando…" : "Entrar"}
          </Button>
        </div>
      </form>
    </MolduraDeAcesso>
  );
}
