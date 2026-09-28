import Link from "next/link";
import { AlternadorDeTema } from "@/components/alternador-de-tema";
import { Marca } from "@/components/marca";
import { AlternadorDeAcesso } from "./alternador-de-acesso";
import { GradeIlustrativa } from "./grade-ilustrativa";

type MolduraDeAcessoProps = {
  titulo: string;
  subtitulo: string;
  ativo: "entrar" | "cadastro";
  children: React.ReactNode;
};

/**
 * Moldura de entrar e cadastro. Mesma rota pros dois perfis, densidades
 * diferentes (docs/identidade-visual.md):
 *
 * - Celular (aluno): uma coluna, e o formulário estica até o fim da tela —
 *   o botão principal fica embaixo, onde o polegar alcança
 * - A partir de lg (professor, no desktop): painel da marca à esquerda com a
 *   prévia do painel da turma
 */
export function MolduraDeAcesso({ titulo, subtitulo, ativo, children }: MolduraDeAcessoProps) {
  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)]">
      <PainelDaMarca />

      <main className="relative flex min-h-dvh flex-col px-6 py-7 lg:justify-center lg:px-12">
        {/* No celular: marca + tema numa linha. No desktop a marca já está no
            painel, e o tema vai pro canto — solto acima do título ele parecia
            um elemento do formulário */}
        <header className="flex items-center justify-between lg:absolute lg:top-8 lg:right-8">
          <Link href="/" className="lg:hidden">
            <Marca />
          </Link>
          <AlternadorDeTema />
        </header>

        <div className="mx-auto mt-8 flex w-full max-w-sm flex-1 flex-col gap-7 lg:mt-0 lg:flex-none">
          <div className="space-y-2">
            <h1 className="font-heading text-3xl font-semibold tracking-tight lg:text-4xl">{titulo}</h1>
            {/* text-pretty evita palavra órfã na última linha ("simulado." sozinho) */}
            <p className="text-muted-foreground text-pretty">{subtitulo}</p>
          </div>

          <AlternadorDeAcesso ativo={ativo} />

          {children}
        </div>
      </main>
    </div>
  );
}

/** Painel de altura inteira: marca no topo, argumento no centro, fonte embaixo. */
function PainelDaMarca() {
  return (
    <aside className="bg-card hidden min-h-dvh flex-col border-r px-12 py-10 lg:flex xl:px-16 xl:py-12 tela-baixa:py-7">
      <Link href="/" className="self-start">
        <Marca />
      </Link>

      <div className="flex flex-1 flex-col justify-center gap-10 py-10 tela-baixa:gap-6 tela-baixa:py-5">
        <div className="space-y-4">
          <p className="text-muted-foreground font-mono text-xs tracking-widest uppercase">
            Para professores e cursinhos
          </p>
          <p className="font-heading max-w-xl text-4xl leading-[1.1] font-semibold tracking-tight xl:text-5xl 2xl:max-w-2xl 2xl:text-6xl tela-baixa:text-4xl">
            A turma inteira numa grade que se lê em{" "}
            {/* O marca-texto no papel dele: grifo sobre o texto, com o
                grafite por cima (11.91 nos dois temas). nowrap: grifo é um
                traço só — quebrado na virada da linha virava dois blocos
                amarelos pesados */}
            <mark className="bg-marca-texto text-sobre-marca-texto rounded-md px-1.5 whitespace-nowrap">
              três segundos
            </mark>
            .
          </p>
        </div>

        <GradeIlustrativa />
      </div>

      {/* Só o que é verdade hoje. Nada de "182 alunos" inventados numa tela
          real — quem lê acredita */}
      <p className="text-muted-foreground font-mono text-xs">
        Questões de todas as provas do ENEM desde 2009
      </p>
    </aside>
  );
}
