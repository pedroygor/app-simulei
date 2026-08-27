package br.com.simulados.api.questao.infrastructure.enemdev;

// DTOs deste pacote espelham o JSON da enem.dev (nomes em inglês, iguais aos
// campos da API). A tradução pro nosso domínio acontece no job de importação,
// nunca aqui — campos que não usamos são simplesmente omitidos
public record ProvaEnemDev(
        int year,
        String title
) {
}
