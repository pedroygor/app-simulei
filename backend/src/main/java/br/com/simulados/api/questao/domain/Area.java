package br.com.simulados.api.questao.domain;

// Os 4 cadernos do ENEM ("discipline" na enem.dev). O JSON da API continua usando
// o nome da constante (MATEMATICA); `nome` é pra mensagens ao usuário
public enum Area {
    LINGUAGENS("Linguagens"),
    CIENCIAS_HUMANAS("Ciências Humanas"),
    CIENCIAS_NATUREZA("Ciências da Natureza"),
    MATEMATICA("Matemática");

    private final String nome;

    Area(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
