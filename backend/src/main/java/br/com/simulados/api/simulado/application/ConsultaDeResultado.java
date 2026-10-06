package br.com.simulados.api.simulado.application;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import br.com.simulados.api.simulado.domain.Placar;
import br.com.simulados.api.simulado.domain.QuestaoDoSimulado;
import br.com.simulados.api.simulado.domain.Resposta;
import br.com.simulados.api.simulado.domain.ResultadoIndisponivelException;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.Tentativa;
import br.com.simulados.api.simulado.domain.TentativaNaoEncontradaException;
import br.com.simulados.api.simulado.infrastructure.RespostaRepository;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O resultado da prova entregue (3c): nota, placar por área e por assunto, e a
 * revisão com gabarito. É o ÚNICO caminho por onde o aluno recebe o gabarito
 * (ADR de 01/10/2026) — por isso a regra "só depois de entregar" mora aqui.
 *
 * A nota não é gravada: é calculada a cada consulta, pra acompanhar a coluna
 * `disponivel` (uma questão anulada depois muda o denominador).
 */
@Service
@RequiredArgsConstructor
public class ConsultaDeResultado {

    private final TentativaRepository tentativaRepository;
    private final SimuladoRepository simuladoRepository;
    private final QuestaoRepository questaoRepository;
    private final RespostaRepository respostaRepository;

    /**
     * @param questoes  na ordem da prova, só as disponíveis, com alternativas e assunto
     * @param marcadas  id da questão → id da alternativa marcada; em branco não aparece
     * @param porArea   na ordem do enum (a ordem fixa do ENEM), só as áreas presentes
     * @param porAssunto do pior aproveitamento pro melhor
     */
    public record Resultado(
            Tentativa tentativa,
            Simulado simulado,
            Placar total,
            Map<Area, Placar> porArea,
            List<LinhaDoResultado> porAssunto,
            List<Questao> questoes,
            Map<Long, Long> marcadas
    ) {
    }

    /** 5 queries fixas: tentativa, simulado, questões, respostas e o GROUP BY. */
    @Transactional(readOnly = true)
    public Resultado detalhar(UUID tentativaId, UUID aluno) {
        var tentativa = tentativaRepository.findByIdAndAlunoId(tentativaId, aluno)
                .orElseThrow(() -> new TentativaNaoEncontradaException(tentativaId));
        if (!tentativa.finalizada()) {
            throw new ResultadoIndisponivelException();
        }
        var simulado = simuladoRepository.buscarComQuestoes(tentativa.getSimuladoId()).orElseThrow();

        var linhas = tentativaRepository.placarPorAssunto(tentativaId);
        var total = linhas.stream().map(LinhaDoResultado::placar).reduce(Placar.VAZIO, Placar::somar);
        // Soma em Java das linhas que já vieram do banco — não é outra consulta
        Map<Area, Placar> porArea = linhas.stream().collect(Collectors.toMap(
                LinhaDoResultado::area, LinhaDoResultado::placar, Placar::somar, () -> new EnumMap<>(Area.class)));
        // sorted é estável: no empate, fica a ordem da taxonomia que veio do banco
        var porAssunto = linhas.stream()
                .sorted(Comparator.comparing(LinhaDoResultado::placar, Placar.PIOR_PRIMEIRO))
                .toList();

        var ids = simulado.getQuestoes().stream().map(QuestaoDoSimulado::getQuestaoId).toList();
        Map<Long, Questao> porId = questaoRepository.buscarVariasParaResultado(ids).stream()
                .collect(Collectors.toMap(Questao::getId, Function.identity()));
        var emOrdem = ids.stream().filter(porId::containsKey).map(porId::get).toList();

        var marcadas = respostaRepository.findByTentativaId(tentativaId).stream()
                .collect(Collectors.toMap(Resposta::getQuestaoId, Resposta::getAlternativaId));

        return new Resultado(tentativa, simulado, total, porArea, porAssunto, emOrdem, marcadas);
    }
}
