package br.com.simulados.api.usuario.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        // max 72: o bcrypt ignora silenciosamente o que passar de 72 bytes,
        // então aceitar senha maior daria falsa sensação de segurança
        @NotBlank @Size(min = 8, max = 72) String senha
        // Sem `role`: todo cadastro é ALUNO, decidido pelo servidor. Um cliente
        // antigo que ainda mande o campo é ignorado (o Boot não falha com campo
        // desconhecido) — nunca é o cliente quem escolhe o próprio papel
) {
}
