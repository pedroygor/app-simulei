package br.com.simulados.api.questao.infrastructure.enemdev;

import java.util.List;

// DTOs deste pacote espelham o JSON da enem.dev (nomes em inglês, iguais aos
// campos da API). A tradução pro nosso domínio acontece no job de importação,
// nunca aqui — campos que não usamos são simplesmente omitidos
public record ProvaEnemDev(
        int year,
        String title,
        // Variantes de língua estrangeira que ESTA prova tem. Varia por ano, e
        // não é detalhe: 2009 não tem nenhuma e 2011 só tem espanhol. Pedir
        // uma variante que a prova não tem devolve 400 ("Language ingles not
        // found in exam") — foi o que abortou a primeira importação completa
        List<IdiomaDaProva> languages
) {

    public record IdiomaDaProva(String label, String value) {
    }

    /** Valores como a API espera no ?language= ("espanhol", "ingles"). */
    public List<String> idiomas() {
        // null-safe: a fonte não tem contrato formal, e campo ausente não
        // pode virar NullPointerException no meio da importação
        return languages == null ? List.of() : languages.stream().map(IdiomaDaProva::value).toList();
    }
}
