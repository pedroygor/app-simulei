package br.com.simulados.api.simulado.application;

import br.com.simulados.api.simulado.domain.ProvaEmAndamento;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** O "Continuar" do painel (4d): as provas abertas do aluno, as mais recentes primeiro. */
@Service
@RequiredArgsConstructor
public class ProvasEmAndamento {

    /** Mais que isso vira lista; o painel é pra decidir rápido o que fazer agora. */
    public static final int MAXIMO = 3;

    private final TentativaRepository tentativaRepository;
    private final Clock clock;

    /**
     * @param agora a hora do servidor, que vai pra tela junto (o celular calcula o
     *              tempo restante pelo desvio do relógio, como no cronômetro da 3d)
     */
    public record Abertas(Instant agora, List<ProvaEmAndamento> provas) {
    }

    /** 1 query, com o teto aplicado no banco. */
    @Transactional(readOnly = true)
    public Abertas doAluno(UUID aluno) {
        var agora = clock.instant();
        return new Abertas(agora, tentativaRepository.emAndamentoDoAluno(aluno, agora, Limit.of(MAXIMO)));
    }
}
