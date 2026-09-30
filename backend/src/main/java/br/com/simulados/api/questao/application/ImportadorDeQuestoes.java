package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.CadernoDoEnem;
import br.com.simulados.api.questao.domain.ChaveNaturalQuestao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.questao.infrastructure.enemdev.ConversorQuestaoEnemDev;
import br.com.simulados.api.questao.infrastructure.enemdev.EnemDevClient;
import br.com.simulados.api.questao.infrastructure.enemdev.ProvaEnemDev;
import br.com.simulados.api.questao.infrastructure.enemdev.QuestaoEnemDev;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Normal é a fonte errar a área de 0 a ~35 questões por ano. Mais que um
    // caderno inteiro de divergência não é erro de etiqueta: é sinal de que a
    // ORDEM dos cadernos mudou e a tabela do CadernoDoEnem precisa de revisão
    private static final int DIVERGENCIAS_QUE_SUGEREM_ORDEM_NOVA = CadernoDoEnem.QUESTOES_POR_AREA;

    private final EnemDevClient client;
    private final ConversorQuestaoEnemDev conversor;
    private final ImagemStorage imagemStorage;
    private final QuestaoRepository questaoRepository;

    public ResultadoImportacao importarAnos(List<Integer> anos) {
        // O catálogo de provas é consultado UMA vez por rodada, não uma por
        // ano: é ele que diz quais variantes de idioma cada prova tem
        var idiomasPorAno = idiomasDeclaradosPorAno();

        var total = ResultadoImportacao.vazio();
        for (var ano : anos) {
            var resultado = importarAno(ano, idiomasPorAno);
            log.info("Ano {} concluído: {}", ano, resultado);
            total = total.mais(resultado);
        }
        return total;
    }

    public ResultadoImportacao importarAno(int ano) {
        return importarAnos(List.of(ano));
    }

    private ResultadoImportacao importarAno(int ano, Map<Integer, List<String>> idiomasPorAno) {
        var idiomas = idiomasPorAno.get(ano);
        if (idiomas == null) {
            // --anos=2030, por exemplo. Sem isto a chamada estouraria 404 lá
            // na fonte e derrubaria a importação dos anos seguintes
            log.warn("A enem.dev não tem a prova de {} — nada a importar", ano);
            return ResultadoImportacao.vazio();
        }
        log.info("Importando questões de {} (variantes de idioma: {})", ano,
                idiomas.isEmpty() ? "nenhuma" : idiomas);

        // UMA query pro ano inteiro. O caminho ingênuo — perguntar ao banco se
        // cada questão existe dentro do laço — seria 185 SELECTs por ano
        // (seção 8.4 do CLAUDE.md).
        Set<ChaveNaturalQuestao> jaImportadas = new HashSet<>(questaoRepository.buscarChavesDoAno(ano));
        Set<ChaveNaturalQuestao> vistasNestaRodada = new HashSet<>();

        var resultado = ResultadoImportacao.vazio();
        for (var origem : buscarQuestoesDeTodasAsVariantes(ano, idiomas)) {
            resultado = resultado.mais(processar(origem, jaImportadas, vistasNestaRodada));
        }
        if (resultado.areasCorrigidas() > DIVERGENCIAS_QUE_SUGEREM_ORDEM_NOVA) {
            log.error("A fonte discordou da posição no caderno em {} questões de {}. Isso é mais que um"
                    + " caderno inteiro: a ordem das áreas pode ter mudado neste ano — revise CadernoDoEnem"
                    + " antes de confiar nas áreas importadas", resultado.areasCorrigidas(), ano);
        }
        return resultado;
    }

    /**
     * A fonte só entrega uma variante de idioma por vez: as questões de inglês
     * repetem o número da versão em espanhol (por isso o idioma faz parte da
     * chave natural), e cada variante exige uma passada própria.
     *
     * As variantes vêm do que a PROVA declara, não de uma lista fixa. A lista
     * fixa ("espanhol", "ingles") funcionou enquanto só 2023 era importado e
     * quebrou na primeira importação completa: 2011 só tem espanhol, e pedir
     * inglês devolve 400. Prova sem variante nenhuma (2009) é varrida uma vez,
     * sem o parâmetro.
     */
    private List<QuestaoEnemDev> buscarQuestoesDeTodasAsVariantes(int ano, List<String> idiomas) {
        if (idiomas.isEmpty()) {
            return buscarTodasAsQuestoes(ano, null);
        }
        var todas = new ArrayList<QuestaoEnemDev>();
        for (var idioma : idiomas) {
            todas.addAll(buscarTodasAsQuestoes(ano, idioma));
        }
        return todas;
    }

    private Map<Integer, List<String>> idiomasDeclaradosPorAno() {
        return client.listarProvas().stream()
                .collect(Collectors.toMap(ProvaEnemDev::year, ProvaEnemDev::idiomas));
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
            return new ResultadoImportacao(0, 1, 0, 0, 0, 0);
        }

        var contadorDeImagens = new ContadorDeImagens();
        try {
            // Imagens são baixadas AQUI, fora de qualquer transação: são
            // centenas de downloads e não podem segurar conexão do pool
            var questao = conversor.converter(origem, contadorDeImagens::guardar);
            var areaCorrigida = areaDaFonteDiverge(origem, questao);
            questaoRepository.save(questao);
            return new ResultadoImportacao(1, 0, 0, contadorDeImagens.guardadas, contadorDeImagens.falhas,
                    areaCorrigida ? 1 : 0);
        } catch (QuestaoInvalidaException e) {
            // Qualidade de dado da fonte: registra e segue. Uma questão
            // defeituosa não pode abortar a importação do ano inteiro.
            log.warn("{}", e.getMessage());
            return new ResultadoImportacao(0, 0, 1, contadorDeImagens.guardadas, contadorDeImagens.falhas, 0);
        }
    }

    // Aviso, não erro: a questão entra com a área da posição. O log existe pra
    // continuarmos medindo a fonte — se ela corrigir o dado, isto zera
    private boolean areaDaFonteDiverge(QuestaoEnemDev origem, Questao questao) {
        var declarada = conversor.areaDeclaradaPelaFonte(origem);
        if (declarada.isPresent() && declarada.get() == questao.getArea()) {
            return false;
        }
        log.warn("{}/{}: a fonte diz {}, a posição no caderno diz {} — usando a posição",
                origem.year(), origem.index(), declarada.map(Enum::name).orElse(origem.discipline()),
                questao.getArea());
        return true;
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
