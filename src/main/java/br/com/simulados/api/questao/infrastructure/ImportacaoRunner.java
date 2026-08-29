package br.com.simulados.api.questao.infrastructure;

import br.com.simulados.api.ImportacaoArgs;
import br.com.simulados.api.questao.application.ImportadorDeQuestoes;
import br.com.simulados.api.questao.infrastructure.enemdev.EnemDevClient;
import br.com.simulados.api.questao.infrastructure.enemdev.ProvaEnemDev;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Comando de importação. O ApplicationRunner é o gancho do Boot pra rodar algo
 * depois que o contexto sobe; sem o argumento --importar-questoes ele não faz
 * nada, então a aplicação web normal não é afetada.
 *
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-questoes --anos=2023"
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--importar-questoes"   (todos os anos)
 */
@Component
@RequiredArgsConstructor
public class ImportacaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoRunner.class);

    private final ImportadorDeQuestoes importador;
    private final EnemDevClient client;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(ImportacaoArgs.FLAG)) {
            return;
        }

        var anos = anosDe(args);
        log.info("=== Importação iniciada: {} ano(s) {} ===", anos.size(), anos);
        var inicio = System.currentTimeMillis();

        var resultado = importador.importarAnos(anos);

        var duracaoSeg = (System.currentTimeMillis() - inicio) / 1000;
        log.info("=== Importação concluída em {}s: {} ===", duracaoSeg, resultado);

        // Job terminou: derruba a aplicação em vez de deixar o Tomcat de pé.
        // SpringApplication.exit fecha o contexto direito (libera pool, flush
        // de logs) antes do System.exit.
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private List<Integer> anosDe(ApplicationArguments args) {
        var valores = args.getOptionValues(ImportacaoArgs.OPCAO_ANOS);
        if (valores == null || valores.isEmpty()) {
            // Sem --anos: importa tudo que a fonte oferece, do mais recente pro
            // mais antigo (prova nova é a que o aluno mais quer treinar)
            return client.listarProvas().stream()
                    .map(ProvaEnemDev::year)
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
        return valores.stream()
                .flatMap(valor -> List.of(valor.split(",")).stream())
                .map(String::trim)
                .filter(texto -> !texto.isBlank())
                .map(Integer::parseInt)
                .toList();
    }
}
