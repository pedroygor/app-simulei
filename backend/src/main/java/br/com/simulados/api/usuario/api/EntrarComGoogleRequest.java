package br.com.simulados.api.usuario.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// "credencial" é o nome que o Google Identity Services dá ao ID token. O teto
// barra corpo gigante antes de chegar ao decoder: um ID token real tem ~1 KB
public record EntrarComGoogleRequest(
        @NotBlank @Size(max = 4096) String credencial
) {
}
