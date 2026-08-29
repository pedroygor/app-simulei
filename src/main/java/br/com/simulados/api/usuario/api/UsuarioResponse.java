package br.com.simulados.api.usuario.api;

import br.com.simulados.api.usuario.domain.Plano;
import br.com.simulados.api.usuario.domain.Role;

import java.time.Instant;
import java.util.UUID;

// Sem senhaHash, de propósito: o que não está no DTO não vaza na API
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        Role role,
        Plano plano,
        Instant criadoEm,
        boolean ativo
) {
}
