import type { Rotulo } from "@/lib/api/questoes";
import { cn } from "@/lib/utils";

/**
 * Assunto da questão, com a matéria na frente quando pedida.
 *
 * No cartão só o assunto: "Probabilidade" diz mais que "Matemática", e a área
 * já está na etiqueta ao lado. No detalhe cabe o caminho inteiro
 * ("Física · Eletrodinâmica"), porque a matéria não é óbvia pelo assunto.
 *
 * Questão ainda não classificada (assunto null) não mostra nada — "sem
 * assunto" seria ruído pro aluno, que não tem o que fazer com a informação.
 */
export function EtiquetaDeAssunto({
  materia,
  assunto,
  comMateria = false,
  className,
}: {
  materia: Rotulo | null;
  assunto: Rotulo | null;
  comMateria?: boolean;
  className?: string;
}) {
  if (!assunto) return null;

  return (
    <span className={cn("text-xs", className)}>
      {comMateria && materia && <>{materia.nome} · </>}
      {assunto.nome}
    </span>
  );
}
