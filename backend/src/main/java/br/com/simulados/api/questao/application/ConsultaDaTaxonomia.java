package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.LinhaDaTaxonomia;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaDaTaxonomia {

    private final AssuntoRepository assuntoRepository;

    // Sem paginação, e é exceção consciente à regra da seção 8.4: a lista é
    // limitada POR CONSTRUÇÃO — 118 assuntos, que só crescem por migration
    @Transactional(readOnly = true)
    // anos vazio = todos. O parâmetro booleano existe porque `q.ano IN ()` com
    // lista vazia não é SQL válido; a lista ganha um valor inócuo nesse caso
    public List<LinhaDaTaxonomia> listar(List<Integer> anos) {
        var todosOsAnos = anos.isEmpty();
        return assuntoRepository.listarComContagem(todosOsAnos, todosOsAnos ? List.of(0) : anos);
    }
}
