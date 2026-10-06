package br.com.simulados.api.simulado.application;

import br.com.simulados.api.shared.exception.NegocioException;

/**
 * O pedido de prova quebra uma regra sobre a lista inteira de escolhas — mais
 * escolhas que questões, escolhas sobrepostas, escolha que não existe na
 * taxonomia. O formato de cada campo já foi validado pelo Bean Validation; isto é
 * o que só o service consegue julgar. 422.
 */
public class PedidoDeSimuladoInvalidoException extends NegocioException {

    public PedidoDeSimuladoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
