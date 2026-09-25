import { cva } from "class-variance-authority";
import Link from "next/link";

const opcao = cva(
  "focus-visible:ring-ring flex h-11 items-center justify-center rounded-lg border text-sm transition-colors focus-visible:ring-2 focus-visible:outline-none",
  {
    variants: {
      ativa: {
        // NEUTRO, sem marca-texto: com a opção ativa em amarelo, a tela tinha
        // dois blocos da cor de destaque competindo (e no tema claro, dois
        // tons diferentes lado a lado). O destaque é do botão principal.
        // Ativa ganha contorno E peso — o estado não depende de cor
        true: "border-contorno bg-card text-foreground font-semibold shadow-sm",
        false: "text-muted-foreground hover:text-foreground border-transparent",
      },
    },
  },
);

/**
 * Alternância entre entrar e criar conta.
 *
 * São LINKS pra duas rotas, não abas. O design usava role="tab", mas o padrão
 * de abas exige tabpanel e navegação por setas — sem isso o ARIA promete um
 * comportamento que não existe. E como são rotas, `/cadastro` continua sendo
 * uma URL de verdade: o link de convite de turma (Etapa 4) vai precisar dela.
 */
export function AlternadorDeAcesso({ ativo }: { ativo: "entrar" | "cadastro" }) {
  return (
    <nav aria-label="Acesso" className="bg-muted grid grid-cols-2 gap-1 rounded-xl p-1">
      <Link
        href="/entrar"
        aria-current={ativo === "entrar" ? "page" : undefined}
        className={opcao({ ativa: ativo === "entrar" })}
      >
        Entrar
      </Link>
      <Link
        href="/cadastro"
        aria-current={ativo === "cadastro" ? "page" : undefined}
        className={opcao({ ativa: ativo === "cadastro" })}
      >
        Criar conta
      </Link>
    </nav>
  );
}
