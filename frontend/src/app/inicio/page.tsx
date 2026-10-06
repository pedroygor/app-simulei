"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { RotaProtegida } from "@/components/auth/rota-protegida";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useSessao } from "@/providers/auth-provider";

/**
 * Área logada. Por enquanto confirma que o ciclo de sessão funciona de ponta a
 * ponta — vai virar a home do produto quando as telas de simulado existirem.
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
        <h1 className="font-heading text-2xl font-bold tracking-tight">Simulei</h1>
        <div className="flex items-center gap-2">
          <AlternadorDeTema />
          <Button variant="outline" onClick={aoSair}>
            Sair
          </Button>
        </div>
      </header>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading">Olá, {usuario?.nome}</CardTitle>
          <CardDescription>Sua sessão está ativa</CardDescription>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-2 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-muted-foreground">E-mail</dt>
              <dd>{usuario?.email}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-muted-foreground">Perfil</dt>
              <dd>{usuario?.role === "PROFESSOR" ? "Professor" : "Aluno"}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-muted-foreground">Plano</dt>
              <dd>{usuario?.plano}</dd>
            </div>
          </dl>
          <div className="mt-6 grid gap-2">
            <Button asChild size="xl" className="w-full">
              <Link href="/simulados/novo">Montar prova</Link>
            </Button>
            <Button asChild variant="outline" className="w-full">
              <Link href="/questoes">Explorar questões</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </main>
  );
}
