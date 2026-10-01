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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Marca as questões incompletas (sem o texto-base ou a imagem de que dependem),
 * a partir do dados/qualidade/questoes_incompletas.csv. Marcada, ela sai da busca,
 * do detalhe, da contagem e do sorteio (coluna gerada `disponivel`, V9).
 *
 * O arquivo é a LISTA COMPLETA, não um acréscimo: questão fora dele fica
 * desmarcada. Assim, desfazer uma marcação é tirar a linha do arquivo — e o banco
 * nunca guarda uma marcação que ninguém consegue achar no repositório.
 *
 * Mesmas garantias das outras cargas: arquivo inteiro validado antes de gravar,
 * idempotente, um SELECT só.
 */
@Service
@RequiredArgsConstructor
public class MarcadorDeIncompletas {

    private final QuestaoRepository questaoRepository;

    // @Transactional: dirty checking (UPDATEs em lote no commit) e atomicidade
    @Transactional
    public ResultadoDasIncompletas marcar(List<LinhaDeIncompleta> linhas) {
        var problemas = new ArrayList<String>();
        var listadas = new HashSet<Chave>();
        for (var linha : linhas) {
            if (!listadas.add(new Chave(linha.ano(), linha.numero(), linha.idioma()))) {
                problemas.add("linha %d: questão %s aparece mais de uma vez".formatted(linha.linha(), linha.chave()));
            }
        }
        if (!problemas.isEmpty()) {
            throw new IncompletasInvalidasException(problemas);
        }

        var questoes = questaoRepository.buscarTodasParaCarga().stream()
                .collect(Collectors.toMap(Chave::de, Function.identity()));

        var semQuestao = linhas.stream()
                .filter(l -> !questoes.containsKey(new Chave(l.ano(), l.numero(), l.idioma())))
                .map(LinhaDeIncompleta::chave)
                .toList();

        int marcadas = 0;
        int liberadas = 0;
        for (var entrada : questoes.entrySet()) {
            var questao = entrada.getValue();
            var deveEstarMarcada = listadas.contains(entrada.getKey());
            if (questao.isIncompleta() == deveEstarMarcada) {
                continue;
            }
            // Só altera o objeto: o UPDATE sai no commit (dirty checking)
            questao.setIncompleta(deveEstarMarcada);
            if (deveEstarMarcada) {
                marcadas++;
            } else {
                liberadas++;
            }
        }
        return new ResultadoDasIncompletas(marcadas, liberadas, questoes.size() - marcadas - liberadas, semQuestao);
    }

    // Chave natural completa: (ano, numero, idioma) — a mesma da UNIQUE da V2
    private record Chave(int ano, int numero, Idioma idioma) {
        static Chave de(Questao questao) {
            return new Chave(questao.getAno(), questao.getNumero(), questao.getIdioma());
        }
    }
}
