package br.com.simulados.api.simulado.application;

import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.QuestaoDoSimulado;
import br.com.simulados.api.simulado.domain.Resposta;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.Tentativa;
import br.com.simulados.api.simulado.domain.TentativaNaoEncontradaException;
import br.com.simulados.api.simulado.infrastructure.RespostaRepository;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultaDeTentativa {

    private final TentativaRepository tentativaRepository;
    private final SimuladoRepository simuladoRepository;
    private final QuestaoRepository questaoRepository;
    private final RespostaRepository respostaRepository;

    /**
     * @param questoes na ordem da prova
     * @param marcadas id da questão → id da alternativa marcada; questão em branco não aparece
     */
    public record Prova(Tentativa tentativa, Simulado simulado, List<Questao> questoes, Map<Long, Long> marcadas) {
    }

    /**
     * A prova inteira de uma vez, em 4 queries fixas com qualquer número de
     * questões: tentativa (filtrada pelo aluno), simulado com a ordem, questões com
     * alternativas (um IN só) e respostas. De uma vez porque, na sala do cursinho
     * com wi-fi ruim, o aluno só precisa de rede pra SALVAR a resposta, não pra ver
     * a próxima questão.
     */
    @Transactional(readOnly = true)
    public Prova prova(UUID tentativaId, UUID aluno) {
        var tentativa = tentativaRepository.findByIdAndAlunoId(tentativaId, aluno)
                .orElseThrow(() -> new TentativaNaoEncontradaException(tentativaId));
        // A FK da V11 garante que o simulado existe
        var simulado = simuladoRepository.buscarComQuestoes(tentativa.getSimuladoId()).orElseThrow();

        var ids = simulado.getQuestoes().stream().map(QuestaoDoSimulado::getQuestaoId).toList();
        // O IN devolve em ordem qualquer; a ordem da prova é a do simulado
        Map<Long, Questao> porId = questaoRepository.buscarVariasComAlternativas(ids).stream()
                .collect(Collectors.toMap(Questao::getId, Function.identity()));
        var emOrdem = ids.stream().map(porId::get).toList();

        var marcadas = respostaRepository.findByTentativaId(tentativaId).stream()
                .collect(Collectors.toMap(Resposta::getQuestaoId, Resposta::getAlternativaId));

        return new Prova(tentativa, simulado, emOrdem, marcadas);
    }
}
