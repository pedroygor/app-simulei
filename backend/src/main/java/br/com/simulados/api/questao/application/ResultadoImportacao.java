package br.com.simulados.api.questao.application;

// Contadores de uma rodada de importação. Job silencioso é job que ninguém
// confia: no fim precisamos saber o que entrou, o que já existia e o que foi
// descartado por qualidade de dado.
public record ResultadoImportacao(
        int importadas,
        int jaExistiam,
        int ignoradasPorDadoInvalido,
        int imagensGuardadas,
        int imagensComFalha,
        // Questões em que o "discipline" da fonte discordava da posição no
        // caderno e a posição venceu (ver CadernoDoEnem)
        int areasCorrigidas
) {

    public static ResultadoImportacao vazio() {
        return new ResultadoImportacao(0, 0, 0, 0, 0, 0);
    }

    public ResultadoImportacao mais(ResultadoImportacao outro) {
        return new ResultadoImportacao(
                importadas + outro.importadas,
                jaExistiam + outro.jaExistiam,
                ignoradasPorDadoInvalido + outro.ignoradasPorDadoInvalido,
                imagensGuardadas + outro.imagensGuardadas,
                imagensComFalha + outro.imagensComFalha,
                areasCorrigidas + outro.areasCorrigidas);
    }

    @Override
    public String toString() {
        return "%d importadas, %d já existiam, %d ignoradas, %d imagens guardadas, %d imagens com falha, %d áreas corrigidas pela posição"
                .formatted(importadas, jaExistiam, ignoradasPorDadoInvalido, imagensGuardadas, imagensComFalha, areasCorrigidas);
    }
}
