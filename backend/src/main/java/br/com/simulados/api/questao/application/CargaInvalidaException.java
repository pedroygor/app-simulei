package br.com.simulados.api.questao.application;

import br.com.simulados.api.shared.exception.RequisicaoInvalidaException;

import java.util.List;

/**
 * Base das cargas que leem um arquivo versionado (classificação por assunto,
 * conferência com o INEP): o arquivo tem linha que não pode entrar no banco.
 *
 * Nessas cargas a regra é a mesma — o arquivo inteiro é recusado ANTES de gravar
 * qualquer coisa, porque um CSV com erro é um CSV que precisa ser corrigido, e
 * carga pela metade deixaria o banco num estado que ninguém consegue descrever.
 * E carrega TODOS os problemas, não só o primeiro: corrigir um por rodada seria
 * tortura com milhares de linhas.
 *
 * Abstrata de propósito: quem captura escolhe a subclasse da carga que falhou,
 * e a mensagem diz de qual carga se trata.
 */
public abstract class CargaInvalidaException extends RequisicaoInvalidaException {

    private static final int MAXIMO_NA_MENSAGEM = 20;

    private final List<String> problemas;

    protected CargaInvalidaException(String titulo, List<String> problemas) {
        super(mensagem(titulo, problemas));
        this.problemas = List.copyOf(problemas);
    }

    public List<String> getProblemas() {
        return problemas;
    }

    private static String mensagem(String titulo, List<String> problemas) {
        var mostrados = problemas.stream().limit(MAXIMO_NA_MENSAGEM).toList();
        var resto = problemas.size() - mostrados.size();
        return "%s recusada, %d problema(s):%n  %s%s".formatted(
                titulo,
                problemas.size(),
                String.join(System.lineSeparator() + "  ", mostrados),
                resto > 0 ? "%n  ... e mais %d".formatted(resto) : "");
    }
}
