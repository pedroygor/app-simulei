package br.com.simulados.api.questao.infrastructure.qualidade;

import br.com.simulados.api.ImportacaoArgs;
import br.com.simulados.api.questao.application.IncompletasInvalidasException;
import br.com.simulados.api.questao.application.MarcadorDeIncompletas;
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
 * Comando que marca as questões incompletas (sem texto-base ou imagem de que
 * dependem). Mesmo esquema dos outros runners: sem a flag, não faz nada.
 *
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--marcar-incompletas"
 *
 * Ordem numa base nova: depois de --importar-questoes (as outras cargas não
 * dependem desta). Não é migration pelo mesmo motivo das outras cargas.
 */
@Component
@RequiredArgsConstructor
public class IncompletasRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IncompletasRunner.class);

    private final LeitorDeIncompletasCsv leitor;
    private final MarcadorDeIncompletas marcador;
    private final ConfigurableApplicationContext context;

    // Caminho padrão no application.yml (relativo a backend/, como o storage)
    @Value("${incompletas.arquivo}")
    private String arquivoPadrao;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(ImportacaoArgs.FLAG_INCOMPLETAS)) {
            return;
        }

        var arquivo = Path.of(arquivo(args));
        log.info("=== Questões incompletas: {} ===", arquivo.toAbsolutePath().normalize());

        int codigoDeSaida;
        try {
            var linhas = leitor.ler(arquivo);
            var resultado = marcador.marcar(linhas);
            log.info("=== Incompletas marcadas ({} linhas): {} ===", linhas.size(), resultado);
            if (!resultado.semQuestao().isEmpty()) {
                log.warn("Sem questão no banco (importe esses anos antes): {}", resultado.semQuestao());
            }
            codigoDeSaida = 0;
        } catch (IncompletasInvalidasException e) {
            // Erro esperado (CSV a corrigir): mensagem limpa, sem stack trace
            log.error(e.getMessage());
            codigoDeSaida = 1;
        }

        // Código de saída diferente de zero: quem rodar num script precisa saber
        var saida = codigoDeSaida;
        System.exit(SpringApplication.exit(context, () -> saida));
    }

    private String arquivo(ApplicationArguments args) {
        var valores = args.getOptionValues(ImportacaoArgs.OPCAO_ARQUIVO);
        return valores == null || valores.isEmpty() ? arquivoPadrao : valores.getFirst();
    }
}
