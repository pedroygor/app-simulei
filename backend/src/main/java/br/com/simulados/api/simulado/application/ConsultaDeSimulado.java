package br.com.simulados.api.simulado.application;

import br.com.simulados.api.simulado.domain.LinhaDaComposicao;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.SimuladoNaoEncontradoException;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultaDeSimulado {

    private final SimuladoRepository simuladoRepository;

    public record Detalhe(Simulado simulado, List<LinhaDaComposicao> composicao) {
    }

    /**
     * Duas queries: o simulado (filtrado pelo dono) e a composição. readOnly:
     * nada é alterado, então o Hibernate dispensa o dirty checking no fim.
     */
    @Transactional(readOnly = true)
    public Detalhe detalhar(UUID id, UUID usuario) {
        var simulado = simuladoRepository.buscarDoUsuario(id, usuario)
                .orElseThrow(() -> new SimuladoNaoEncontradoException(id));
        return new Detalhe(simulado, simuladoRepository.composicao(id));
    }
}
