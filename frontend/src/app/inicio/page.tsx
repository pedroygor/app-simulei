"use client";

import { useRouter } from "next/navigation";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { RotaProtegida } from "@/components/auth/rota-protegida";
import { PainelDoAluno } from "@/components/painel/painel-do-aluno";
import { Button } from "@/components/ui/button";
import { useSessao } from "@/providers/auth-provider";

/**
 * A home do aluno logado: o painel (4d). A saudação usa só o primeiro nome — o
 * completo não cabe ao lado dos botões a 360px.
 */
export default function PaginaInicial() {
  return (
    <RotaProtegida>
      <ConteudoDaHome />
    </RotaProtegida>
  );
}

function ConteudoDaHome() {
  const router = useRouter();
  const { usuario, sair } = useSessao();

  async function aoSair() {
    await sair();
    router.replace("/entrar");
  }

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-8">
      <header className="mb-8 flex items-center justify-between gap-4">
        <h1 className="font-heading min-w-0 truncate text-2xl font-bold tracking-tight">
          Olá, {usuario?.nome.split(" ")[0]}
        </h1>
        <div className="flex shrink-0 items-center gap-2">
          <AlternadorDeTema />
          <Button variant="outline" onClick={aoSair}>
            Sair
          </Button>
        </div>
      </header>
      <PainelDoAluno />
    </main>
  );
}
