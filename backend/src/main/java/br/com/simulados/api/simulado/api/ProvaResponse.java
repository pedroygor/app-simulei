package br.com.simulados.api.simulado.api;

import br.com.simulados.api.questao.api.AlternativaResponse;
import br.com.simulados.api.questao.api.QuestaoMapper;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.simulado.application.ConsultaDeTentativa.Prova;
import br.com.simulados.api.simulado.domain.StatusDaTentativa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * A prova pra responder. O que NÃO vem, de propósito:
 *   - gabarito e `correta`: só no resultado da prova finalizada (3c)
 *   - assunto e habilidade: dariam dica ("Probabilidade" já diz por onde começar),
 *     como foi decidido pro detalhe da questão na 2b.8
 */
public record ProvaResponse(
        UUID tentativaId,
        UUID simuladoId,
        String titulo,
        // null = sem cronômetro
        Integer tempoLimiteSeg,
        StatusDaTentativa status,
        Instant iniciadoEm,
        // Até quando dá pra responder; null = sem cronômetro. O servidor recusa
        // resposta depois dele (com 10 s de folga)
        Instant prazo,
        // Hora do servidor nesta resposta: a tela calcula o tempo restante como
        // prazo − agora, sem confiar no relógio do celular
        Instant agora,
        Instant finalizadoEm,
        List<QuestaoDaProvaResponse> questoes
) {

    public record QuestaoDaProvaResponse(
            // Posição na prova (1, 2, 3...), não o número no caderno do ENEM
            int ordem,
            Long id,
            Integer ano,
            Integer numero,
            Area area,
            Idioma idioma,
            String enunciado,
            String comando,
            List<AlternativaResponse> alternativas,
            // id da alternativa marcada; null = em branco
            Long marcada
    ) {
    }

    // O mapper das alternativas é o mesmo do detalhe da questão: uma forma só de
    // representar alternativa na API
    public static ProvaResponse de(Prova prova, QuestaoMapper mapper) {
        var tentativa = prova.tentativa();
        var questoes = IntStream.range(0, prova.questoes().size())
                .mapToObj(i -> {
                    var q = prova.questoes().get(i);
                    return new QuestaoDaProvaResponse(i + 1, q.getId(), q.getAno(), q.getNumero(), q.getArea(),
                            q.getIdioma(), q.getEnunciado(), q.getComando(),
                            mapper.toAlternativas(q.getAlternativas()), prova.marcadas().get(q.getId()));
                })
                .toList();
        return new ProvaResponse(tentativa.getId(), prova.simulado().getId(), prova.simulado().getTitulo(),
                prova.simulado().getTempoLimiteSeg(), tentativa.getStatus(), tentativa.getIniciadoEm(),
                tentativa.getPrazo(), prova.agora(), tentativa.getFinalizadoEm(), questoes);
    }
}
