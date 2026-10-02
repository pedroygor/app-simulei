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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grava em cada questão a habilidade oficial (H1–H30) e a dificuldade TRI
 * (parâmetro B) publicadas pelo INEP, a partir do habilidades.csv gerado por
 * ferramentas/inep/gerar_habilidades.py.
 *
 * Mesmas garantias das outras cargas de arquivo:
 * 1. Tudo ou nada: valida TODAS as linhas antes de alterar qualquer questão.
 * 2. Idempotente: rodar de novo com o mesmo arquivo não altera nada.
 * 3. Um SELECT só, seja o arquivo de 10 ou de 2.729 linhas.
 *
 * Sem regra de "não sobrescrever": ao contrário do assunto, a habilidade não é
 * decisão nossa, é cópia do dado oficial. O arquivo manda.
 */
@Service
@RequiredArgsConstructor
public class ImportadorDeHabilidades {

    private final QuestaoRepository questaoRepository;

    /**
     * @Transactional: dirty checking (basta alterar os campos, os UPDATEs saem no
     * commit, em lotes) e atomicidade (se algo estourar, nada fica pela metade).
     * A FK composta da V8 é a última barreira: código fora de 1–30 nem chega aqui
     * (o leitor recusa), e a área é conferida abaixo.
     */
    @Transactional
    public ResultadoDasHabilidades importar(List<LinhaDeHabilidade> linhas) {
        var questoesPorChave = questaoRepository.buscarTodasParaCarga().stream()
                .collect(Collectors.toMap(Chave::de, Function.identity()));

        var problemas = new ArrayList<String>();
        var semQuestao = new ArrayList<String>();
        var pedidas = new ArrayList<Alteracao>();
        var vistas = new HashSet<Chave>();

        for (var linha : linhas) {
            var chave = new Chave(linha.ano(), linha.numero(), linha.idioma());
            if (!vistas.add(chave)) {
                problemas.add("linha %d: questão %s aparece mais de uma vez".formatted(linha.linha(), linha.chave()));
                continue;
            }
            var questao = questoesPorChave.get(chave);
            if (questao == null) {
                semQuestao.add(linha.chave());
                continue;
            }
            // Área diferente = o casamento por posição caiu na questão errada. Gravar
            // seria pior que não ter: a tela mostraria habilidade de outra prova
            if (questao.getArea() != linha.area()) {
                problemas.add("linha %d: questão %s é de %s, mas o INEP diz %s".formatted(
                        linha.linha(), linha.chave(), questao.getArea(), linha.area()));
                continue;
            }
            pedidas.add(new Alteracao(questao, linha));
        }

        if (!problemas.isEmpty()) {
            throw new HabilidadesInvalidasException(problemas);
        }

        int atualizadas = 0;
        for (var alteracao : pedidas) {
            var questao = alteracao.questao();
            var linha = alteracao.linha();
            if (Objects.equals(questao.getHabilidade(), linha.habilidade())
                    && Objects.equals(questao.getDificuldade(), linha.dificuldade())) {
                continue;
            }
            // Só altera o objeto: o UPDATE sai no commit (dirty checking)
            questao.setHabilidade(linha.habilidade());
            questao.setDificuldade(linha.dificuldade());
            atualizadas++;
        }
        return new ResultadoDasHabilidades(atualizadas, pedidas.size() - atualizadas, List.copyOf(semQuestao));
    }

    // Chave natural completa: (ano, numero, idioma) — a mesma da UNIQUE da V2
    private record Chave(int ano, int numero, Idioma idioma) {
        static Chave de(Questao questao) {
            return new Chave(questao.getAno(), questao.getNumero(), questao.getIdioma());
        }
    }

    private record Alteracao(Questao questao, LinhaDeHabilidade linha) {
    }
}
