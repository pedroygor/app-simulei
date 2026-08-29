package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.ChaveNaturalQuestao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.questao.infrastructure.enemdev.ConversorQuestaoEnemDev;
import br.com.simulados.api.questao.infrastructure.enemdev.EnemDevClient;
import br.com.simulados.api.questao.infrastructure.enemdev.QuestaoEnemDev;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Puxa as questões da enem.dev e persiste no nosso banco.
 *
 * Idempotente: rodar de novo não duplica nada. A garantia vem em três camadas —
 * as chaves já importadas são carregadas antes do laço, as chaves vistas na
 * própria rodada são deduplicadas em memória, e a UNIQUE do banco é o backstop.
 */
@Service
@RequiredArgsConstructor
public class ImportadorDeQuestoes {

    private static final Logger log = LoggerFactory.getLogger(ImportadorDeQuestoes.class);

    // Teto da API: limit > 50 devolve 400
    private static final int TAMANHO_DA_PAGINA = 50;

    // A fonte só entrega uma variante de idioma por vez. Sem a segunda passada
    // perderíamos as questões de inglês — elas repetem o número da versão em
    // espanhol, e por isso o idioma faz parte da chave natural.
    private static final List<String> IDIOMAS_DA_FONTE = List.of("espanhol", "ingles");

    private final EnemDevClient client;
    private final ConversorQuestaoEnemDev conversor;
    private final ImagemStorage imagemStorage;
    private final QuestaoRepository questaoRepository;

    public ResultadoImportacao importarAnos(List<Integer> anos) {
        var total = ResultadoImportacao.vazio();
        for (var ano : anos) {
            var resultado = importarAno(ano);
            log.info("Ano {} concluído: {}", ano, resultado);
            total = total.mais(resultado);
        }
        return total;
    }

    public ResultadoImportacao importarAno(int ano) {
        log.info("Importando questões de {}", ano);

        // UMA query pro ano inteiro. O caminho ingênuo — perguntar ao banco se
        // cada questão existe dentro do laço — seria 185 SELECTs por ano
        // (seção 8.4 do CLAUDE.md).
        Set<ChaveNaturalQuestao> jaImportadas = new HashSet<>(questaoRepository.buscarChavesDoAno(ano));
        Set<ChaveNaturalQuestao> vistasNestaRodada = new HashSet<>();

        var resultado = ResultadoImportacao.vazio();
        for (var idioma : IDIOMAS_DA_FONTE) {
            for (var origem : buscarTodasAsQuestoes(ano, idioma)) {
                resultado = resultado.mais(
                        processar(origem, jaImportadas, vistasNestaRodada));
            }
        }
        return resultado;
    }

    private ResultadoImportacao processar(QuestaoEnemDev origem,
                                          Set<ChaveNaturalQuestao> jaImportadas,
                                          Set<ChaveNaturalQuestao> vistasNestaRodada) {
        var chave = new ChaveNaturalQuestao(origem.index(), idiomaDe(origem.language()));

        // A segunda passada de idioma retrás as ~180 questões sem idioma, e a
        // paginação da fonte repete a última questão de cada página (ver
        // buscarTodasAsQuestoes). Os dois casos caem aqui.
        if (!vistasNestaRodada.add(chave)) {
            return ResultadoImportacao.vazio();
        }
        if (jaImportadas.contains(chave)) {
            return new ResultadoImportacao(0, 1, 0, 0, 0);
        }

        var contadorDeImagens = new ContadorDeImagens();
        try {
            // Imagens são baixadas AQUI, fora de qualquer transação: são
            // centenas de downloads e não podem segurar conexão do pool
            var questao = conversor.converter(origem, contadorDeImagens::guardar);
            questaoRepository.save(questao);
            return new ResultadoImportacao(1, 0, 0, contadorDeImagens.guardadas, contadorDeImagens.falhas);
        } catch (QuestaoInvalidaException e) {
            // Qualidade de dado da fonte: registra e segue. Uma questão
            // defeituosa não pode abortar a importação do ano inteiro.
            log.warn("{}", e.getMessage());
            return new ResultadoImportacao(0, 0, 1, contadorDeImagens.guardadas, contadorDeImagens.falhas);
        }
    }

    /**
     * Pagina até o fim de um ano.
     *
     * Cuidado com a fonte: o offset da enem.dev é inclusivo, então uma página
     * com offset=50 devolve os índices 50..100 — 51 itens, repetindo o último
     * da página anterior. Avançamos pelo que a API realmente entregou e a
     * deduplicação por chave natural cuida da sobreposição.
     */
    private List<QuestaoEnemDev> buscarTodasAsQuestoes(int ano, String idioma) {
        var todas = new ArrayList<QuestaoEnemDev>();
        var offset = 0;
        while (true) {
            var pagina = client.buscarQuestoes(ano, TAMANHO_DA_PAGINA, offset, idioma);
            var questoes = pagina.questions();
            if (questoes == null || questoes.isEmpty()) {
                return todas;
            }
            todas.addAll(questoes);
            if (!pagina.metadata().hasMore()) {
                return todas;
            }
            offset += TAMANHO_DA_PAGINA;
        }
    }

    private Idioma idiomaDe(String language) {
        if (language == null) {
            return null;
        }
        return "ingles".equals(language) ? Idioma.INGLES : Idioma.ESPANHOL;
    }

    // Guarda imagem contando sucessos e recusas. Nenhuma das duas derruba a
    // questão: perder o texto inteiro por causa de um PNG fora do ar (ou de um
    // SVG que recusamos) seria pior.
    private final class ContadorDeImagens {
        private int guardadas;
        private int falhas;

        private String guardar(String urlOrigem) {
            try {
                var urlLocal = imagemStorage.guardar(urlOrigem);
                guardadas++;
                return urlLocal;
            } catch (ImagemIndisponivelException | ImagemRejeitadaException e) {
                // Fallback deliberado: mantemos a URL da FONTE. A questão
                // continua utilizável, e o conteúdo recusado (um SVG, por
                // exemplo) passa a ser servido pelo domínio deles, não pelo
                // nosso — que é exatamente o ponto da recusa.
                log.warn("{} — mantendo URL da fonte", e.getMessage());
                falhas++;
                return urlOrigem;
            }
        }
    }
}
