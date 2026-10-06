import { z } from "zod";
import { IDIOMAS } from "@/lib/api/questoes";
import { TIPOS_DE_ESCOLHA } from "@/lib/api/simulados";

/**
 * Espelha o Bean Validation do CriarSimuladoRequest. Validar no cliente é
 * conveniência (resposta imediata); quem vale é o backend.
 *
 * Campo numérico vazio chega como undefined, não NaN: a tela registra os campos
 * com `numeroOuVazio` (abaixo) em vez de `valueAsNumber`.
 */

/** Pro `setValueAs` do register: "" vira undefined (ausente), o resto vira número. */
export const numeroOuVazio = (valor: unknown) => (valor === "" || valor === undefined ? undefined : Number(valor));

export const esquemaDeMontagem = z.object({
  titulo: z.string().trim().min(1, "Dê um nome à prova").max(120, "Nome muito longo"),
  total: z
    .number({ error: "Informe quantas questões" })
    .int("Número inteiro")
    .min(5, "A prova precisa de pelo menos 5 questões")
    .max(180, "No máximo 180 questões"),
  anos: z.array(z.number().int()).max(20),
  idioma: z.enum(IDIOMAS).optional(),
  itens: z
    .array(
      z.object({
        tipo: z.enum(TIPOS_DE_ESCOLHA),
        valor: z.string().min(1),
        rotulo: z.string(),
        quantidade: z.number().int("Número inteiro").min(1, "Pelo menos 1").max(180, "No máximo 180").optional(),
      }),
    )
    .min(1, "Escolha pelo menos uma área, matéria ou assunto")
    .max(30, "No máximo 30 escolhas"),
  // Minutos na tela (1 a 360, o mesmo intervalo do backend em segundos)
  tempoLimiteMin: z.number().int("Número inteiro").min(1, "Pelo menos 1 minuto").max(360, "No máximo 6 horas").optional(),
});

export type EntradaDaMontagem = z.input<typeof esquemaDeMontagem>;
export type FormularioValidado = z.output<typeof esquemaDeMontagem>;
