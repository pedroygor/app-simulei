package br.com.simulados.api.desempenho.application;

import br.com.simulados.api.desempenho.domain.Aproveitamento;
import br.com.simulados.api.desempenho.domain.SemanaDeDesempenho;
import br.com.simulados.api.desempenho.infrastructure.DesempenhoRepository;
import br.com.simulados.api.desempenho.infrastructure.LinhaDaSemana;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.AssuntoNaoEncontradoException;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.simulado.domain.Placar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * "Estou melhorando?" (4c): o aproveitamento por semana, geral e por área, nas
 * últimas N semanas. Mesma métrica da 4a (acerto sobre as RESPONDIDAS), pra que os
 * números daqui batam com os do topo da tela.
 *
 * Só por semana, não por prova: cada prova tem conteúdo diferente, e "prova 1: 40%,
 * prova 2: 70%" pareceria melhora quando só a prova foi mais fácil. Na semana,
 * várias provas se somam e a mistura dilui (ADR de 20/10/2026).
 */
@Service
@RequiredArgsConstructor
public class EvolucaoDoDesempenho {

    /** O fuso das semanas: o do aluno, não o do servidor (que pode estar em UTC). */
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public static final int SEMANAS_PADRAO = 4;

    private final DesempenhoRepository repository;
    private final AssuntoRepository assuntoRepository;
    private final Clock clock;

    /**
     * Uma query, qualquer que seja a janela. A janela é de semanas INTEIRAS: começa
     * na segunda-feira de N−1 semanas atrás, e a semana atual (incompleta) entra.
     * Toda semana da janela volta, inclusive as vazias, pra a tela manter o eixo.
     */
    @Transactional(readOnly = true)
    public List<SemanaDeDesempenho> doAluno(UUID aluno, int semanas) {
        return semanas(aluno, semanas, null);
    }

    /**
     * A evolução de UM assunto (4c): "treinei, melhorei?". 2 queries: o assunto pelo
     * slug (com a matéria, pra tela) e a mesma agregação, filtrada. Slug inexistente é
     * 404, não evolução vazia — vazia diria "você nunca respondeu", quando o assunto
     * nem existe.
     */
    @Transactional(readOnly = true)
    public EvolucaoNoAssunto doAlunoNoAssunto(UUID aluno, int semanas, String slug) {
        var assunto = assuntoRepository.buscarPorSlugComMateria(slug)
                .orElseThrow(() -> new AssuntoNaoEncontradoException(slug));
        return new EvolucaoNoAssunto(assunto, semanas(aluno, semanas, assunto.getId()));
    }

    public record EvolucaoNoAssunto(Assunto assunto, List<SemanaDeDesempenho> semanas) {
    }

    private List<SemanaDeDesempenho> semanas(UUID aluno, int semanas, Integer assunto) {
        var estaSemana = LocalDate.now(clock.withZone(FUSO)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var primeira = estaSemana.minusWeeks(semanas - 1L);
        var linhas = repository.semanasDoAluno(aluno, primeira.atStartOfDay(FUSO).toInstant(), assunto);

        // Semana → área → aproveitamento; TreeMap/EnumMap mantêm as duas ordens
        Map<LocalDate, Map<Area, Aproveitamento>> porSemana = new TreeMap<>();
        for (LinhaDaSemana linha : linhas) {
            porSemana.computeIfAbsent(linha.getSemana(), s -> new EnumMap<>(Area.class))
                    .put(Area.valueOf(linha.getArea()), Aproveitamento.de(
                            new Placar(linha.getQuestoes(), linha.getRespondidas(), linha.getAcertos())));
        }

        var resultado = new ArrayList<SemanaDeDesempenho>(semanas);
        for (var inicio = primeira; !inicio.isAfter(estaSemana); inicio = inicio.plusWeeks(1)) {
            var areas = porSemana.getOrDefault(inicio, Map.of());
            var geral = areas.values().stream().reduce(Aproveitamento.VAZIO, Aproveitamento::somar);
            resultado.add(new SemanaDeDesempenho(inicio, geral, areas.isEmpty() ? Map.of() : areas));
        }
        return resultado;
    }
}
