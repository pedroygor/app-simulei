package br.com.simulados.api.desempenho.application;

import br.com.simulados.api.desempenho.domain.Aproveitamento;
import br.com.simulados.api.desempenho.domain.AproveitamentoNoAssunto;
import br.com.simulados.api.desempenho.infrastructure.DesempenhoRepository;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O desempenho do aluno somando todas as provas entregues (4a): geral, por área e
 * por assunto. Ranking de pontos fracos e "treinar este assunto" são da 4b.
 */
@Service
@RequiredArgsConstructor
public class ConsultaDeDesempenho {

    private final DesempenhoRepository repository;

    /**
     * @param provas     quantas provas entregues entraram na conta
     * @param porArea    na ordem do enum (a do ENEM), só as áreas com alguma questão
     * @param porAssunto na ordem da taxonomia, só as questões classificadas
     */
    public record Desempenho(
            long provas,
            Aproveitamento geral,
            Map<Area, Aproveitamento> porArea,
            List<AproveitamentoNoAssunto> porAssunto
    ) {
    }

    /**
     * 2 queries fixas, qualquer que seja o número de provas: o GROUP BY e a contagem.
     * readOnly: as duas leituras saem do mesmo snapshot e o Hibernate não procura
     * mudança pra gravar no fim.
     */
    @Transactional(readOnly = true)
    public Desempenho doAluno(UUID aluno) {
        var linhas = repository.linhasDoAluno(aluno);

        // Geral e áreas são somas em Java das linhas que já vieram — não outra consulta
        var geral = linhas.stream()
                .map(l -> Aproveitamento.de(l.placar()))
                .reduce(Aproveitamento.VAZIO, Aproveitamento::somar);
        Map<Area, Aproveitamento> porArea = linhas.stream().collect(Collectors.toMap(
                LinhaDoResultado::area, l -> Aproveitamento.de(l.placar()), Aproveitamento::somar,
                () -> new EnumMap<>(Area.class)));

        // Questão sem assunto conta no geral e na área, mas não vira linha de assunto:
        // "Sem assunto: 40%" não é diagnóstico de nada que o aluno possa treinar
        var porAssunto = linhas.stream()
                .filter(l -> l.assuntoSlug() != null)
                .map(l -> new AproveitamentoNoAssunto(l.area(), l.materiaSlug(), l.materiaNome(),
                        l.assuntoSlug(), l.assuntoNome(), Aproveitamento.de(l.placar())))
                .toList();

        return new Desempenho(repository.provasEntregues(aluno), geral, porArea, porAssunto);
    }
}
