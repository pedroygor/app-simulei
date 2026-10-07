package br.com.simulados.api.simulado.api;

import br.com.simulados.api.questao.api.AlternativaResponse;
import br.com.simulados.api.questao.api.QuestaoMapper;
import br.com.simulados.api.questao.api.RotuloResponse;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.simulado.application.ConsultaDeResultado.Resultado;
import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import br.com.simulados.api.simulado.domain.Placar;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * O resultado da prova entregue. Tipo SEPARADO do ProvaResponse de propósito: o
 * gabarito só existe aqui, então não há refatoração da prova que o vaze — o
 * campo nem existe lá (ADR de 01/10/2026).
 */
public record ResultadoResponse(
        UUID tentativaId,
        UUID simuladoId,
        String titulo,
        Instant finalizadoEm,
        // A prova acabou pelo cronômetro (entregue no prazo pelo front ou pelo
        // job), não pelo aluno: a tela diz "Tempo esgotado"
        boolean tempoEsgotado,
        PlacarResponse total,
        // Na ordem fixa do ENEM, só as áreas que estavam na prova
        List<PorArea> porArea,
        // Do pior aproveitamento pro melhor
        List<PorAssunto> porAssunto,
        List<QuestaoCorrigida> questoes
) {

    /** percentual null = nenhuma questão (não 0%: não houve o que errar). */
    public record PlacarResponse(long questoes, long respondidas, long acertos, long emBranco, Integer percentual) {

        static PlacarResponse de(Placar placar) {
            return new PlacarResponse(placar.questoes(), placar.respondidas(), placar.acertos(),
                    placar.emBranco(), placar.percentual());
        }
    }

    public record PorArea(Area area, String nome, PlacarResponse placar) {
    }

    // materia e assunto null = questão ainda não classificada
    public record PorAssunto(Area area, RotuloResponse materia, RotuloResponse assunto, PlacarResponse placar) {

        static PorAssunto de(LinhaDoResultado linha) {
            return new PorAssunto(linha.area(),
                    linha.materiaSlug() == null ? null : new RotuloResponse(linha.materiaSlug(), linha.materiaNome()),
                    linha.assuntoSlug() == null ? null : new RotuloResponse(linha.assuntoSlug(), linha.assuntoNome()),
                    PlacarResponse.de(linha.placar()));
        }
    }

    /**
     * Uma questão da revisão. Agora o assunto vai junto (durante a prova ele daria
     * dica; depois de entregar, explica o que errou). `correta` não vai: é só
     * comparar `marcada` com a alternativa da letra do gabarito.
     */
    public record QuestaoCorrigida(
            // Posição na prova, a mesma numeração que o aluno viu
            int ordem,
            Long id,
            Integer ano,
            Integer numero,
            Area area,
            Idioma idioma,
            RotuloResponse materia,
            RotuloResponse assunto,
            String enunciado,
            String comando,
            List<AlternativaResponse> alternativas,
            // id da alternativa marcada; null = em branco
            Long marcada,
            // Letra, como no detalhe da questão
            String gabarito
    ) {
    }

    public static ResultadoResponse de(Resultado resultado, QuestaoMapper mapper) {
        var porArea = resultado.porArea().entrySet().stream()
                .map(e -> new PorArea(e.getKey(), e.getKey().getNome(), PlacarResponse.de(e.getValue())))
                .toList();
        var questoes = IntStream.range(0, resultado.questoes().size())
                .mapToObj(i -> {
                    var q = resultado.questoes().get(i);
                    var assunto = q.getAssunto();
                    return new QuestaoCorrigida(i + 1, q.getId(), q.getAno(), q.getNumero(), q.getArea(),
                            q.getIdioma(),
                            assunto == null ? null : mapper.toRotulo(assunto.getMateria()),
                            mapper.toRotulo(assunto),
                            q.getEnunciado(), q.getComando(), mapper.toAlternativas(q.getAlternativas()),
                            resultado.marcadas().get(q.getId()), q.getGabarito());
                })
                .toList();
        var tentativa = resultado.tentativa();
        return new ResultadoResponse(tentativa.getId(), resultado.simulado().getId(), resultado.simulado().getTitulo(),
                tentativa.getFinalizadoEm(), tentativa.encerradaPeloTempo(), PlacarResponse.de(resultado.total()), porArea,
                resultado.porAssunto().stream().map(PorAssunto::de).toList(), questoes);
    }
}
