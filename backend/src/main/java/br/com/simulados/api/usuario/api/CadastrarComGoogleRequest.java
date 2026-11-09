package br.com.simulados.api.usuario.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// O e-mail não vem do cliente: sai do token conferido. O nome vem, porque a
// pessoa confirma (ou corrige) o que o Google sugeriu. Mesmos limites do
// cadastro por senha
public record CadastrarComGoogleRequest(
        @NotBlank @Size(max = 4096) String credencial,
        @NotBlank @Size(max = 120) String nome
) {
}
