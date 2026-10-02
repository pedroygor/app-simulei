package br.com.simulados.api.simulado.application;

import br.com.simulados.api.simulado.domain.Simulado;

import java.util.List;

/**
 * O simulado salvo e quanto saiu de cada escolha ("Ciências da Natureza: 15").
 * A composição não é guardada no banco: é a resposta do sorteio, mostrada logo
 * depois da criação pra quem montou conferir o que pediu.
 */
public record SimuladoCriado(Simulado simulado, List<Parte> composicao) {

    public record Parte(String rotulo, int quantidade) {
    }
}
