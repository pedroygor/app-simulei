package br.com.simulados.api.questao.api;

public record AlternativaResponse(
        Long id,
        String letra,
        String texto,
        String imagemUrl
) {
}
