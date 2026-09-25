import { resolverUrlDeImagem } from "@/lib/api/url";
import { cn } from "@/lib/utils";

/**
 * Imagem de enunciado ou de alternativa. Um componente só pros dois usos: o
 * markdown do enunciado embute imagem, e 715 alternativas são SÓ imagem —
 * resolver a URL e o texto alternativo em dois lugares seria pedir divergência.
 */
export function ImagemDaQuestao({
  src,
  alt,
  className,
}: {
  src: string;
  alt: string;
  className?: string;
}) {
  return (
    // <img> e não next/image de propósito: o Image exige dimensão conhecida e
    // host declarado em next.config, e aqui são centenas de imagens de
    // terceiro, de tamanho arbitrário, servidas pela nossa própria API. O
    // ganho de otimização não paga o acoplamento.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={resolverUrlDeImagem(src)}
      alt={alt}
      // Questão pode ter várias imagens e o aluno está no 4G do interior
      loading="lazy"
      decoding="async"
      className={cn("h-auto max-w-full rounded-md", className)}
    />
  );
}
