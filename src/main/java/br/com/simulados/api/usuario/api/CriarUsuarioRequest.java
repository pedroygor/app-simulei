package br.com.simulados.api.usuario.api;

import br.com.simulados.api.usuario.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarUsuarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        // max 72: o bcrypt ignora silenciosamente o que passar de 72 bytes,
        // então aceitar senha maior daria falsa sensação de segurança
        @NotBlank @Size(min = 8, max = 72) String senha,
        @NotNull Role role
) {
}
