package br.com.simulados.api.simulado.application;

import br.com.simulados.api.simulado.domain.Tentativa;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Entrega as provas com tempo vencido que ninguém entregou (3d). O caso normal é
 * o front entregar quando o cronômetro zera; este job cobre o aluno que fechou o
 * app no meio — sem ele a prova ficaria "em andamento" pra sempre no histórico.
 *
 * @Scheduled(fixedDelay): roda de novo X depois do FIM da execução anterior (não
 * a cada X fixo), então duas execuções nunca se sobrepõem. Só existe com o
 * agendamento ligado (AgendamentoConfig), isto é, com a API web no ar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncerramentoDeProvasVencidas {

    private final TentativaRepository tentativaRepository;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${provas.encerramento-automatico.intervalo:PT1M}")
    @Transactional
    public int encerrar() {
        var encerradas = tentativaRepository.encerrarVencidas(clock.instant().minus(Tentativa.TOLERANCIA));
        if (encerradas > 0) {
            log.info("{} prova(s) com tempo vencido entregue(s) automaticamente", encerradas);
        }
        return encerradas;
    }
}
