package br.com.simulados.api.simulado.api;

import br.com.simulados.api.simulado.application.SimuladoCriado;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resposta da criação: o que foi montado e quanto saiu de cada escolha. Sem as
 * questões e, sobretudo, sem gabarito — quem cria pode ser o aluno, e a prova é
 * aberta pela tentativa (3b).
 */
public record SimuladoCriadoResponse(
        UUID id,
        String titulo,
        int total,
        Integer tempoLimiteSeg,
        Instant criadoEm,
        List<Parte> composicao
) {

    public record Parte(String rotulo, int quantidade) {
    }

    static SimuladoCriadoResponse de(SimuladoCriado criado) {
        var simulado = criado.simulado();
        return new SimuladoCriadoResponse(simulado.getId(), simulado.getTitulo(), simulado.getQuestoes().size(),
                simulado.getTempoLimiteSeg(), simulado.getCriadoEm(),
                criado.composicao().stream().map(p -> new Parte(p.rotulo(), p.quantidade())).toList());
    }
}
