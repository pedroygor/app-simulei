package br.com.simulados.api;

// Nome do argumento compartilhado entre o main (que decide se sobe servidor
// web) e o runner (que decide se importa). Constante única pra não dessincronizar.
public final class ImportacaoArgs {

    public static final String FLAG = "importar-questoes";
    public static final String OPCAO_ANOS = "anos";

    private ImportacaoArgs() {
    }
}
