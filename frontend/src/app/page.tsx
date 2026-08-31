"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useSessao } from "@/providers/auth-provider";

/**
 * Porta de entrada: manda pra home logada ou pro login.
 *
 * Precisa ser client component porque a sessão só é conhecida no navegador —
 * o access token está em memória e o cookie de refresh é httpOnly.
 */
export default function Raiz() {
  const router = useRouter();
  const { usuario, carregando } = useSessao();

  useEffect(() => {
    if (carregando) return;
    router.replace(usuario ? "/inicio" : "/entrar");
  }, [carregando, usuario, router]);

  return (
    <div className="flex min-h-dvh items-center justify-center">
      <p role="status" aria-live="polite" className="text-muted-foreground text-sm">
        Carregando…
      </p>
    </div>
  );
}
