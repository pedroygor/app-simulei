import Markdown from "react-markdown";
import { resolverUrlDeImagem } from "@/lib/api/url";
import { cn } from "@/lib/utils";

/**
 * Renderiza o markdown de enunciado, comando ou alternativa.
 *
 * ---------------------------------------------------------------------------
 * POR QUE ESTE COMPONENTE EXISTE, E POR QUE NINGUÉM DEVE RENDERIZAR MARKDOWN
 * DE QUESTÃO FORA DELE
 * ---------------------------------------------------------------------------
 * O texto vem da enem.dev, que NÃO sanitiza nada. Medido na fonte: a
 * alternativa B de 2009 q105 contém, literalmente,
 *
 *     “</DIV> </SPAN> <BR CLEAR = ALL> < BR> <BR> <SCRIPT>”
 *
 * É lixo de OCR e inofensivo em si, mas prova que HTML arbitrário atravessa a
 * fonte até nós. É o mesmo risco de cadeia de suprimento das imagens (seção
 * 8.6 do CLAUDE.md), só que no texto.
 *
 * A defesa aqui é o PADRÃO da biblioteca, não uma opção que alguém liga:
 * o react-markdown ignora HTML cru a menos que se adicione `rehype-raw`.
 * Nunca adicione. E note que não há `dangerouslySetInnerHTML` em lugar
 * nenhum deste caminho — o markdown vira elemento React direto, então nem um
 * erro meu consegue transformar aquele `<SCRIPT>` em tag de verdade.
 */

/**
 * Só estes elementos passam. É defesa em profundidade: o que segura o HTML cru
 * é a ausência do rehype-raw, isto aqui é a segunda tranca.
 *
 * `unwrapDisallowed` preserva o TEXTO do que for barrado. Importa porque só
 * medimos 2023 (imagens, negrito, itálico) — se um ano futuro trouxer tabela
 * ou lista, o aluno perde a formatação, nunca o conteúdo.
 */
const ELEMENTOS_PERMITIDOS = [
  "p", "br", "em", "strong", "img",
  "ul", "ol", "li", "blockquote", "code", "pre",
  "sub", "sup", "h1", "h2", "h3", "h4", "h5", "h6",
];

type Props = {
  texto: string | null | undefined;
  className?: string;
};

export function TextoDaQuestao({ texto, className }: Props) {
  if (!texto?.trim()) return null;

  return (
    <div className={cn("space-y-3 leading-relaxed", className)}>
      <Markdown
        allowedElements={ELEMENTOS_PERMITIDOS}
        unwrapDisallowed
        components={{ img: ImagemDaQuestao }}
      >
        {texto}
      </Markdown>
    </div>
  );
}

/**
 * O `src` chega aqui JÁ higienizado pelo urlTransform padrão do
 * react-markdown, que barra protocolo perigoso (`javascript:`). O que falta é
 * resolver o caminho relativo contra a base da API — ver resolverUrlDeImagem.
 */
function ImagemDaQuestao({ src, alt }: { src?: string | Blob; alt?: string }) {
  if (typeof src !== "string" || !src) return null;

  return (
    // <img> e não next/image de propósito: o Image exige dimensão conhecida e
    // host declarado em next.config, e aqui são centenas de imagens de
    // terceiro, de tamanho arbitrário, servidas pela nossa própria API. O
    // ganho de otimização não paga o acoplamento.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={resolverUrlDeImagem(src)}
      // A fonte quase sempre manda alt vazio (`![]()`). Um rótulo genérico ao
      // menos anuncia que existe conteúdo visual ali; descrever de verdade
      // exigiria curadoria humana e está fora de escopo.
      alt={alt?.trim() || "Imagem da questão"}
      // Questão pode ter várias imagens e o aluno está no 4G do interior
      loading="lazy"
      decoding="async"
      className="mx-auto h-auto max-w-full rounded-md"
    />
  );
}
