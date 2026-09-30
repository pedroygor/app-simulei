package br.com.simulados.api;

import java.util.List;

// Nome do argumento compartilhado entre o main (que decide se sobe servidor
// web) e o runner (que decide se importa). Constante única pra não dessincronizar.
public final class ImportacaoArgs {

    public static final String FLAG = "importar-questoes";
    public static final String OPCAO_ANOS = "anos";

    public static final String FLAG_CLASSIFICACAO = "importar-classificacao";
    public static final String OPCAO_ARQUIVO = "arquivo";

    private ImportacaoArgs() {
    }

    // Qualquer job de linha de comando roda sem servidor web
    public static boolean ehJob(String[] args) {
        var lista = List.of(args);
        return lista.contains("--" + FLAG) || lista.contains("--" + FLAG_CLASSIFICACAO);
    }
}
