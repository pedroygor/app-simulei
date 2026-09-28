import { z } from "zod";

/**
 * Espelham as regras do Bean Validation no backend (CriarUsuarioRequest e
 * LoginRequest). Validar no cliente é conveniência — dá resposta imediata sem
 * ida ao servidor. A validação que VALE é a do backend, que o cliente não
 * contorna. Quando as duas divergirem, o backend manda.
 */

const email = z
  .string()
  .min(1, "Informe seu e-mail")
  .email("E-mail inválido")
  .max(255, "E-mail muito longo");

export const esquemaDeLogin = z.object({
  email,
  senha: z.string().min(1, "Informe sua senha"),
});

export const esquemaDeCadastro = z.object({
  nome: z.string().min(1, "Informe seu nome").max(120, "Nome muito longo"),
  email,
  // min 8 e max 72 vêm do backend; o teto é do bcrypt, que ignora o que
  // passar de 72 bytes — aceitar mais daria falsa sensação de segurança
  senha: z
    .string()
    .min(8, "A senha precisa de pelo menos 8 caracteres")
    .max(72, "A senha pode ter no máximo 72 caracteres"),
  role: z.enum(["ALUNO", "PROFESSOR"]),
});

export type FormularioDeLogin = z.infer<typeof esquemaDeLogin>;
export type FormularioDeCadastro = z.infer<typeof esquemaDeCadastro>;
