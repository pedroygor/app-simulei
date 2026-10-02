package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.Idioma;

/**
 * Uma linha do dados/conferencia-inep/correcoes.csv, já convertida. O serviço
 * recebe isto e não o arquivo: ler CSV é detalhe de infraestrutura, e o teste do
 * serviço monta as linhas direto, sem arquivo em disco.
 *
 * `linha` é o número da linha no arquivo, só pra mensagem de erro apontar onde
 * corrigir. `valorBanco` é o que a enem.dev entregou (a letra errada, ou a letra
 * de uma questão que o INEP anulou); `valorInep` é o que o INEP diz (a letra
 * certa, ou X = anulada).
 */
public record LinhaDeConferencia(
        int linha,
        TipoDeCorrecao tipo,
        int ano,
        int numero,
        Idioma idioma,
        String valorBanco,
        String valorInep
) {

    public String chave() {
        return "%d/%d%s".formatted(ano, numero, idioma == null ? "" : "/" + idioma);
    }
}
