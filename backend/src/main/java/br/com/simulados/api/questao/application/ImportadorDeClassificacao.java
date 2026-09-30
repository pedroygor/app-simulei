package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carrega no banco o assunto de cada questão, a partir das linhas do
 * dados/classificacao/assuntos.csv (classificação feita pela IA — ADR de
 * 29/09/2026).
 *
 * Três garantias:
 * 1. Tudo ou nada: valida TODAS as linhas antes de alterar qualquer questão.
 * 2. Idempotente: rodar de novo com o mesmo arquivo não altera nada.
 * 3. Nunca sobrescreve REVISAO_HUMANA.
 *
 * Número fixo de SELECTs (dois), seja o arquivo de 10 ou de 2.741 linhas.
 */
@Service
@RequiredArgsConstructor
public class ImportadorDeClassificacao {

    private final QuestaoRepository questaoRepository;
    private final AssuntoRepository assuntoRepository;

    /**
     * @Transactional aqui faz duas coisas:
     * - dirty checking: as questões carregadas ficam "gerenciadas" pelo
     *   Hibernate, e basta alterar os campos — no commit ele compara com o
     *   estado original e gera os UPDATEs sozinho. Nenhum save() por linha.
     * - atomicidade: se algo estourar no meio da escrita, o rollback desfaz
     *   o que já tinha sido alterado.
     */
    @Transactional
    public ResultadoDaClassificacao importar(List<LinhaDeClassificacao> linhas) {
        // Dado de referência, 118 linhas: carregar tudo é mais barato que
        // buscar slug a slug
        var assuntosPorSlug = assuntoRepository.findAll().stream()
                .collect(Collectors.toMap(Assunto::getSlug, Function.identity()));
        var questoesPorChave = questaoRepository.buscarTodasParaClassificacao().stream()
                .collect(Collectors.toMap(Chave::de, Function.identity()));

        var validacao = validar(linhas, assuntosPorSlug, questoesPorChave);
        return aplicar(validacao.pedidas(), validacao.semQuestao());
    }

    private Validacao validar(List<LinhaDeClassificacao> linhas,
                              Map<String, Assunto> assuntosPorSlug,
                              Map<Chave, Questao> questoesPorChave) {
        var problemas = new ArrayList<String>();
        var semQuestao = new ArrayList<String>();
        var pedidas = new ArrayList<Alteracao>();
        var chavesVistas = new HashSet<Chave>();

        for (var linha : linhas) {
            var chave = new Chave(linha.ano(), linha.numero(), linha.idioma());

            // Duas linhas pra mesma questão: qual vale? Não cabe ao job decidir
            if (!chavesVistas.add(chave)) {
                problemas.add("linha %d: questão %s repetida no arquivo".formatted(linha.linha(), linha.chave()));
                continue;
            }

            var assunto = assuntosPorSlug.get(linha.assunto());
            if (assunto == null) {
                problemas.add("linha %d: assunto '%s' não existe na taxonomia".formatted(linha.linha(), linha.assunto()));
                continue;
            }

            var questao = questoesPorChave.get(chave);
            if (questao == null) {
                semQuestao.add(linha.chave());
                continue;
            }

            // O banco também recusaria (FK composta da V4), mas com uma
            // DataIntegrityViolation genérica no flush, sem dizer a linha
            if (questao.getArea() != assunto.getArea()) {
                problemas.add("linha %d: questão %s é de %s, mas '%s' é de %s".formatted(
                        linha.linha(), linha.chave(), questao.getArea(), assunto.getSlug(), assunto.getArea()));
                continue;
            }

            pedidas.add(new Alteracao(questao, assunto, linha.confianca()));
        }

        if (!problemas.isEmpty()) {
            throw new ClassificacaoInvalidaException(problemas);
        }
        return new Validacao(pedidas, semQuestao);
    }

    private ResultadoDaClassificacao aplicar(List<Alteracao> alteracoes, List<String> semQuestao) {
        int atualizadas = 0;
        int inalteradas = 0;
        int preservadas = 0;

        for (var alteracao : alteracoes) {
            var questao = alteracao.questao();

            if (questao.getClassificacaoOrigem() == OrigemDaClassificacao.REVISAO_HUMANA) {
                preservadas++;
            } else if (jaEstaIgual(questao, alteracao)) {
                inalteradas++;
            } else {
                // Só altera o objeto: o UPDATE sai no commit (dirty checking)
                questao.setAssunto(alteracao.assunto());
                questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
                questao.setClassificacaoConfianca(alteracao.confianca());
                atualizadas++;
            }
        }

        return new ResultadoDaClassificacao(atualizadas, inalteradas, preservadas, List.copyOf(semQuestao));
    }

    // Compara pelo id: ler o id de um proxy lazy não dispara SELECT
    private boolean jaEstaIgual(Questao questao, Alteracao alteracao) {
        var atual = questao.getAssunto();
        return atual != null
                && Objects.equals(atual.getId(), alteracao.assunto().getId())
                && questao.getClassificacaoOrigem() == OrigemDaClassificacao.IA
                && questao.getClassificacaoConfianca() == alteracao.confianca();
    }

    // Chave natural completa: (ano, numero, idioma) — a mesma da UNIQUE da V2
    private record Chave(int ano, int numero, Idioma idioma) {
        static Chave de(Questao questao) {
            return new Chave(questao.getAno(), questao.getNumero(), questao.getIdioma());
        }
    }

    private record Alteracao(Questao questao, Assunto assunto, ConfiancaDaClassificacao confianca) {
    }

    private record Validacao(List<Alteracao> pedidas, List<String> semQuestao) {
    }
}
