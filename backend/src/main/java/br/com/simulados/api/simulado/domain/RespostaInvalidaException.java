package br.com.simulados.api.simulado.domain;

import br.com.simulados.api.shared.exception.NegocioException;

/**
 * A questão não é desta prova, ou a alternativa não é desta questão. Uma exceção
 * só pros dois casos: o cliente honesto nunca manda nenhum deles (a tela só
 * oferece as alternativas da questão), e distinguir custaria outra query.
 */
public class RespostaInvalidaException extends NegocioException {

    public RespostaInvalidaException(Long questao, Long alternativa) {
        super("A alternativa " + alternativa + " não é uma opção da questão " + questao + " desta prova");
    }
}
