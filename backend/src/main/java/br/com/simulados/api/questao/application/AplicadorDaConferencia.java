package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Aplica no banco as correções que o INEP (fonte oficial) faz sobre o que a
 * enem.dev entregou: gabarito trocado e questão anulada. O INEP prevalece —
 * decidido em 30/09/2026; ver docs/conferencia-inep.md.
 *
 * Mesmas garantias da carga da classificação:
 * 1. Tudo ou nada: valida TODAS as linhas antes de alterar qualquer questão.
 * 2. Idempotente: rodar de novo com o mesmo arquivo não altera nada.
 * 3. Não atropela edição posterior: só troca o gabarito se ele ainda for o que
 *    o arquivo viu; do contrário a linha vai pra `divergentes`.
 *
 * Um SELECT só, seja o arquivo de 10 ou de 2.741 linhas.
 */
@Service
@RequiredArgsConstructor
public class AplicadorDaConferencia {

    private static final Pattern LETRA = Pattern.compile("[A-E]");
    private static final String ANULADA_NO_INEP = "X";

    private final QuestaoRepository questaoRepository;

    /**
     * @Transactional: as questões carregadas ficam gerenciadas pelo Hibernate e
     * basta alterar os campos — no commit ele compara com o estado original e
     * gera os UPDATEs (dirty checking), em lotes (hibernate.jdbc.batch_size).
     * Também dá a atomicidade: se algo estourar no meio, o rollback desfaz tudo.
     */
    @Transactional
    public ResultadoDaConferencia aplicar(List<LinhaDeConferencia> linhas) {
        validar(linhas);

        var questoesPorChave = questaoRepository.buscarTodasParaCarga().stream()
                .collect(Collectors.toMap(Chave::de, Function.identity()));

        int corrigidos = 0;
        int anuladas = 0;
        int inalteradas = 0;
        var divergentes = new ArrayList<String>();
        var semQuestao = new ArrayList<String>();

        for (var linha : linhas) {
            var questao = questoesPorChave.get(new Chave(linha.ano(), linha.numero(), linha.idioma()));
            if (questao == null) {
                semQuestao.add(linha.chave());
                continue;
            }

            switch (linha.tipo()) {
                case ANULADA -> {
                    if (questao.isAnulada()) {
                        inalteradas++;
                    } else {
                        // Anulada não tem gabarito (CHECK da V2: anulada OR gabarito NOT NULL).
                        // Fica registrada, em vez de apagada: a próxima importação da enem.dev
                        // pula questão que já existe pela chave natural; apagar faria ela voltar
                        questao.setAnulada(true);
                        questao.setGabarito(null);
                        anuladas++;
                    }
                }
                case GABARITO -> {
                    var atual = questao.getGabarito();
                    if (questao.isAnulada() || linha.valorInep().equals(atual)) {
                        // Anulada não tem gabarito a corrigir; já correto é o caso da 2ª rodada
                        inalteradas++;
                    } else if (linha.valorBanco().equals(atual)) {
                        questao.setGabarito(linha.valorInep());
                        corrigidos++;
                    } else {
                        divergentes.add("linha %d, questão %s: o banco tem '%s', o arquivo esperava '%s' e propõe '%s'"
                                .formatted(linha.linha(), linha.chave(), atual, linha.valorBanco(), linha.valorInep()));
                    }
                }
            }
        }

        return new ResultadoDaConferencia(corrigidos, anuladas, inalteradas,
                List.copyOf(divergentes), List.copyOf(semQuestao));
    }

    // Regras sobre o arquivo inteiro, ANTES de qualquer escrita
    private void validar(List<LinhaDeConferencia> linhas) {
        var problemas = new ArrayList<String>();
        var vistas = new HashSet<Chave>();

        for (var linha : linhas) {
            // Duas linhas pra mesma questão: qual vale? Não cabe ao job decidir
            if (!vistas.add(new Chave(linha.ano(), linha.numero(), linha.idioma()))) {
                problemas.add("linha %d: questão %s aparece mais de uma vez".formatted(linha.linha(), linha.chave()));
            }

            switch (linha.tipo()) {
                case GABARITO -> {
                    if (!LETRA.matcher(linha.valorInep()).matches()) {
                        problemas.add("linha %d: gabarito do INEP deve ser A–E, veio '%s'"
                                .formatted(linha.linha(), linha.valorInep()));
                    }
                    if (!LETRA.matcher(linha.valorBanco()).matches()) {
                        problemas.add("linha %d: gabarito anterior deve ser A–E, veio '%s'"
                                .formatted(linha.linha(), linha.valorBanco()));
                    }
                }
                case ANULADA -> {
                    // O INEP escreve a anulação como X; aceitar qualquer outra coisa
                    // deixaria uma linha de GABARITO mal classificada anular questão
                    if (!ANULADA_NO_INEP.equals(linha.valorInep())) {
                        problemas.add("linha %d: anulação deve ter valor_inep = X, veio '%s'"
                                .formatted(linha.linha(), linha.valorInep()));
                    }
                }
            }
        }

        if (!problemas.isEmpty()) {
            throw new ConferenciaInvalidaException(problemas);
        }
    }

    // Chave natural completa: (ano, numero, idioma) — a mesma da UNIQUE da V2
    private record Chave(int ano, int numero, Idioma idioma) {
        static Chave de(Questao questao) {
            return new Chave(questao.getAno(), questao.getNumero(), questao.getIdioma());
        }
    }
}
