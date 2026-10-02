package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.Idioma;

/**
 * Uma linha do dados/qualidade/questoes_incompletas.csv: questão que a fonte
 * entregou sem o texto-base ou a imagem de que a pergunta depende. O `motivo` é
 * pra quem lê o arquivo; não vai pro banco.
 */
public record LinhaDeIncompleta(int linha, int ano, int numero, Idioma idioma) {

    public String chave() {
        return "%d/%d%s".formatted(ano, numero, idioma == null ? "" : "/" + idioma);
    }
}
