package br.com.simulados.api.simulado.application;

import br.com.simulados.api.shared.exception.NegocioException;

import java.util.List;

/**
 * Não há questões pra montar a prova pedida (nos anos pedidos): uma escolha sem
 * nenhuma questão, uma quantidade fixa maior que o disponível, ou o total maior
 * que tudo o que existe nas escolhas. 422 pelo GlobalExceptionHandler (base
 * NegocioException).
 *
 * Junta TODAS as faltas numa mensagem só: quem monta a prova ajusta tudo de uma
 * vez, em vez de descobrir um problema por tentativa.
 */
public class QuestoesInsuficientesException extends NegocioException {

    private final List<String> faltas;

    public QuestoesInsuficientesException(List<String> faltas) {
        super("Não há questões suficientes: " + String.join("; ", faltas));
        this.faltas = List.copyOf(faltas);
    }

    public List<String> getFaltas() {
        return faltas;
    }
}
