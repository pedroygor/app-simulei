import Link from "next/link";
import { Button } from "@/components/ui/button";

/** O resto do app. "Montar prova" é a ação principal; as outras, secundárias. */
export function AcoesDoPainel() {
  return (
    <nav aria-label="Atalhos" className="grid gap-2">
      <Button asChild size="xl" className="w-full">
        <Link href="/simulados/novo">Montar prova</Link>
      </Button>
      <Button asChild variant="outline" className="w-full">
        <Link href="/provas">Minhas provas</Link>
      </Button>
      <Button asChild variant="outline" className="w-full">
        <Link href="/questoes">Explorar questões</Link>
      </Button>
    </nav>
  );
}
