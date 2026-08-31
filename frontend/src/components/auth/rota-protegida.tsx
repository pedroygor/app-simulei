"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useSessao } from "@/providers/auth-provider";

/**
 * Esconde a tela de quem não tem sessão e manda pro login.
 *
 * É proteção de NAVEGAÇÃO, não de dados: quem desabilitar o JS ou chamar a API
 * direto passa por aqui. A proteção real está no backend, onde toda rota
 * (menos cadastro, login e imagens) exige token — aqui é só pra não mostrar
 * esqueleto de tela que nunca vai carregar.
 */
export function RotaProtegida({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { usuario, carregando } = useSessao();

  useEffect(() => {
    if (!carregando && !usuario) {
      // replace e não push: o botão voltar não deve retornar pra tela protegida
      router.replace("/entrar");
    }
  }, [carregando, usuario, router]);

  if (carregando) {
    return (
      <div className="flex min-h-dvh items-center justify-center">
        {/* aria-live faz o leitor de tela anunciar que está carregando */}
        <p role="status" aria-live="polite" className="text-muted-foreground text-sm">
          Carregando…
        </p>
      </div>
    );
  }

  if (!usuario) {
    return null;
  }

  return <>{children}</>;
}
