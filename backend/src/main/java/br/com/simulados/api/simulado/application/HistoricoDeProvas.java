package br.com.simulados.api.simulado.application;

import br.com.simulados.api.simulado.domain.ItemDoHistorico;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** "Minhas provas" (3d): o que o usuário montou, em que pé está e a nota de cada uma. */
@Service
@RequiredArgsConstructor
public class HistoricoDeProvas {

    private final SimuladoRepository simuladoRepository;

    /**
     * Só as provas do usuário (filtro na query). A ordem é fixa — da mais nova
     * pra mais velha — e o sort que vier do cliente é DESCARTADO: o Spring Data
     * concatenaria `?sort=` no JPQL sem validar (o bug de 26/08/2026 na busca de
     * questões). Fica só a página e o tamanho.
     */
    @Transactional(readOnly = true)
    public Page<ItemDoHistorico> listar(UUID usuario, Pageable pagina) {
        return simuladoRepository.historicoDoUsuario(usuario, PageRequest.of(pagina.getPageNumber(), pagina.getPageSize()));
    }
}
