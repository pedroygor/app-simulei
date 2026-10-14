package br.com.simulados.api.simulado.application;

import br.com.simulados.api.simulado.domain.RespostaInvalidaException;
import br.com.simulados.api.simulado.domain.SimuladoNaoEncontradoException;
import br.com.simulados.api.simulado.domain.Tentativa;
import br.com.simulados.api.simulado.domain.TentativaNaoEncontradaException;
import br.com.simulados.api.simulado.infrastructure.RespostaRepository;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.simulado.infrastructure.TentativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Fazer a prova (3b): iniciar, responder, desmarcar, finalizar. Todo método
 * recebe o aluno autenticado e a autorização acontece dentro das queries — uma
 * tentativa alheia nunca chega a ser carregada.
 *
 * Nenhum método devolve `correta` nem gabarito: a correção só aparece com a prova
 * finalizada (3c).
 */
@Service
@RequiredArgsConstructor
public class ExecucaoDaProva {

    private final TentativaRepository tentativaRepository;
    private final RespostaRepository respostaRepository;
    private final SimuladoRepository simuladoRepository;
    private final Clock clock;

    // `nova` deixa o controller escolher entre 201 (criou) e 200 (já existia)
    public record TentativaIniciada(Tentativa tentativa, boolean nova) {
    }

    /**
     * Inicia a prova, ou devolve a que já está em andamento (ou finalizada): é
     * assim que o aluno que fechou o navegador continua de onde parou. 2 queries,
     * qualquer que seja a corrida entre cliques — ver criarSeNaoExistir.
     */
    @Transactional
    public TentativaIniciada iniciar(UUID simulado, UUID aluno) {
        var inseridas = tentativaRepository.criarSeNaoExistir(simulado, aluno);
        // Nem inseriu nem achou: o simulado não existe ou não é do aluno
        var tentativa = tentativaRepository.findBySimuladoIdAndAlunoId(simulado, aluno)
                .orElseThrow(() -> new SimuladoNaoEncontradoException(simulado));
        return new TentativaIniciada(tentativa, inseridas == 1);
    }

    /**
     * Marca (ou troca) a alternativa da questão. 3 queries: tentativa travada,
     * validação + correção, upsert.
     */
    @Transactional
    public void responder(UUID tentativaId, UUID aluno, Long questao, Long alternativa) {
        var tentativa = tentativaParaEscrita(tentativaId, aluno);
        tentativa.exigirDentroDoPrazo(clock.instant());
        var correta = simuladoRepository.alternativaEstaCorreta(tentativa.getSimuladoId(), questao, alternativa)
                .orElseThrow(() -> new RespostaInvalidaException(questao, alternativa));
        respostaRepository.registrar(tentativaId, questao, alternativa, correta);
    }

    /**
     * Deixa a questão em branco. Idempotente: desmarcar o que não estava marcado
     * (ou uma questão de fora da prova) não apaga nada e não é erro — o DELETE só
     * alcança linhas desta tentativa, que já é do aluno.
     */
    @Transactional
    public void desmarcar(UUID tentativaId, UUID aluno, Long questao) {
        // Desmarcar também é mudar a prova: depois do prazo, não
        tentativaParaEscrita(tentativaId, aluno).exigirDentroDoPrazo(clock.instant());
        respostaRepository.apagar(tentativaId, questao);
    }

    /**
     * Entrega a prova. O UPDATE sai pelo dirty checking no commit: a entidade foi
     * carregada nesta transação, e o Hibernate grava o que mudou nela.
     *
     * Sem checar o prazo, de propósito: entregar depois do tempo é justamente o
     * que o front faz quando o cronômetro zera (e o horário gravado é o prazo).
     */
    @Transactional
    public Tentativa finalizar(UUID tentativaId, UUID aluno) {
        var tentativa = tentativaParaEscrita(tentativaId, aluno);
        tentativa.finalizar(clock.instant());
        return tentativa;
    }

    // Travada (FOR UPDATE) e só se for do aluno; prova entregue não aceita escrita
    private Tentativa tentativaParaEscrita(UUID tentativaId, UUID aluno) {
        var tentativa = tentativaRepository.buscarDoAlunoParaEscrita(tentativaId, aluno)
                .orElseThrow(() -> new TentativaNaoEncontradaException(tentativaId));
        tentativa.exigirEmAndamento();
        return tentativa;
    }
}
