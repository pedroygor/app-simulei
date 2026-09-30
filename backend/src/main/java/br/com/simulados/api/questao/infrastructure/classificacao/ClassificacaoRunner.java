package br.com.simulados.api.questao.infrastructure.classificacao;

import br.com.simulados.api.ImportacaoArgs;
import br.com.simulados.api.questao.application.ClassificacaoInvalidaException;
import br.com.simulados.api.questao.application.ImportadorDeClassificacao;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Comando de carga da classificação por assunto. Mesmo esquema do
 * ImportacaoRunner: sem a flag, não faz nada.
 *
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-classificacao"
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-classificacao --arquivo=/outro/caminho.csv"
 */
@Component
@RequiredArgsConstructor
public class ClassificacaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClassificacaoRunner.class);

    private final LeitorDeClassificacaoCsv leitor;
    private final ImportadorDeClassificacao importador;
    private final ConfigurableApplicationContext context;

    // Caminho padrão no application.yml (relativo a backend/, como o storage)
    @Value("${classificacao.arquivo}")
    private String arquivoPadrao;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(ImportacaoArgs.FLAG_CLASSIFICACAO)) {
            return;
        }

        var arquivo = Path.of(arquivo(args));
        log.info("=== Carga da classificação: {} ===", arquivo.toAbsolutePath().normalize());

        int codigoDeSaida;
        try {
            var linhas = leitor.ler(arquivo);
            var resultado = importador.importar(linhas);
            log.info("=== Carga concluída ({} linhas): {} ===", linhas.size(), resultado);
            if (!resultado.semQuestao().isEmpty()) {
                log.warn("Sem questão no banco (importe esses anos antes): {}", resultado.semQuestao());
            }
            codigoDeSaida = 0;
        } catch (ClassificacaoInvalidaException e) {
            // Erro esperado (CSV a corrigir): mensagem limpa, sem stack trace
            log.error(e.getMessage());
            codigoDeSaida = 1;
        }

        // Código de saída diferente de zero: quem rodar isso num script
        // precisa saber que a carga foi recusada
        var saida = codigoDeSaida;
        System.exit(SpringApplication.exit(context, () -> saida));
    }

    private String arquivo(ApplicationArguments args) {
        var valores = args.getOptionValues(ImportacaoArgs.OPCAO_ARQUIVO);
        return valores == null || valores.isEmpty() ? arquivoPadrao : valores.getFirst();
    }
}
