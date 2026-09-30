package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.Idioma;

/**
 * Uma linha do dados/classificacao/assuntos.csv, já convertida. O service
 * recebe isto e não o arquivo: ler CSV é detalhe de infraestrutura, e o teste
 * do service monta as linhas direto, sem arquivo em disco.
 *
 * `linha` é o número da linha no arquivo, só pra mensagem de erro apontar
 * onde corrigir.
 */
public record LinhaDeClassificacao(
        int linha,
        int ano,
        int numero,
        Idioma idioma,
        String assunto,
        ConfiancaDaClassificacao confianca
) {

    public String chave() {
        return "%d/%d%s".formatted(ano, numero, idioma == null ? "" : "/" + idioma);
    }
}
