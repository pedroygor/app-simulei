package br.com.simulados.api.questao.infrastructure.conferencia;

import br.com.simulados.api.ImportacaoArgs;
import br.com.simulados.api.questao.application.AplicadorDaConferencia;
import br.com.simulados.api.questao.application.ConferenciaInvalidaException;
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
 * Comando que aplica as correções do INEP (gabarito e anulações). Mesmo esquema
 * dos outros runners: sem a flag, não faz nada.
 *
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--aplicar-conferencia-inep"
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--aplicar-conferencia-inep --arquivo=/outro/caminho.csv"
 *
 * Ordem numa base nova: --importar-questoes, --importar-classificacao e só então
 * este. Não é migration porque as questões são importadas DEPOIS das migrations:
 * um UPDATE por migration não acharia nada numa base zerada.
 */
@Component
@RequiredArgsConstructor
public class ConferenciaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConferenciaRunner.class);

    private final LeitorDeConferenciaCsv leitor;
    private final AplicadorDaConferencia aplicador;
    private final ConfigurableApplicationContext context;

    // Caminho padrão no application.yml (relativo a backend/, como o storage)
    @Value("${conferencia-inep.arquivo}")
    private String arquivoPadrao;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(ImportacaoArgs.FLAG_CONFERENCIA)) {
            return;
        }

        var arquivo = Path.of(arquivo(args));
        log.info("=== Conferência com o INEP: {} ===", arquivo.toAbsolutePath().normalize());

        int codigoDeSaida;
        try {
            var linhas = leitor.ler(arquivo);
            var resultado = aplicador.aplicar(linhas);
            log.info("=== Conferência aplicada ({} linhas): {} ===", linhas.size(), resultado);
            if (!resultado.semQuestao().isEmpty()) {
                log.warn("Sem questão no banco (importe esses anos antes): {}", resultado.semQuestao());
            }
            // Divergente não é erro do arquivo nem do banco: é alguém que mexeu depois.
            // Sai com código 0, mas em voz alta — é decisão humana
            resultado.divergentes().forEach(d -> log.warn("Não tocada — {}", d));
            codigoDeSaida = 0;
        } catch (ConferenciaInvalidaException e) {
            // Erro esperado (CSV a corrigir): mensagem limpa, sem stack trace
            log.error(e.getMessage());
            codigoDeSaida = 1;
        }

        // Código de saída diferente de zero: quem rodar isso num script precisa
        // saber que a carga foi recusada
        var saida = codigoDeSaida;
        System.exit(SpringApplication.exit(context, () -> saida));
    }

    private String arquivo(ApplicationArguments args) {
        var valores = args.getOptionValues(ImportacaoArgs.OPCAO_ARQUIVO);
        return valores == null || valores.isEmpty() ? arquivoPadrao : valores.getFirst();
    }
}
