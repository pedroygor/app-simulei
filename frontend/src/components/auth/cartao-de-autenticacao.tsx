import Link from "next/link";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type CartaoDeAutenticacaoProps = {
  titulo: string;
  descricao: string;
  children: React.ReactNode;
  rodape: React.ReactNode;
};

/**
 * Moldura compartilhada por entrar e cadastro. Mobile-first: numa tela de
 * 360px o cartão ocupa a largura toda; a partir de sm ele centraliza e ganha
 * limite de largura.
 */
export function CartaoDeAutenticacao({
  titulo,
  descricao,
  children,
  rodape,
}: CartaoDeAutenticacaoProps) {
  return (
    <main className="flex min-h-dvh flex-col px-4 py-8 sm:justify-center">
      <div className="mx-auto w-full max-w-sm">
        <div className="mb-6 flex items-center justify-between">
          <Link href="/" className="font-heading text-2xl font-bold tracking-tight">
            Simulei
          </Link>
          <AlternadorDeTema />
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="font-heading text-xl">{titulo}</CardTitle>
            <CardDescription>{descricao}</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-6">
            {children}
            <p className="text-muted-foreground text-center text-sm">{rodape}</p>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
