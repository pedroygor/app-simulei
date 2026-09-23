package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.domain.QuestaoNaoEncontradaException;
import br.com.simulados.api.questao.domain.ResumoDeQuestao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuscaDeQuestoes {

    private final QuestaoRepository questaoRepository;

    // readOnly=true: sem dirty checking e sem flush no fim — o Hibernate nem
    // guarda snapshot das entidades carregadas
    @Transactional(readOnly = true)
    public Page<ResumoDeQuestao> buscar(Area area, Idioma idioma, Integer anoInicial,
                                        Integer anoFinal, String materia, Pageable pageable) {
        return questaoRepository.buscar(area, idioma, anoInicial, anoFinal, materia, pageable);
    }

    @Transactional(readOnly = true)
    public Questao buscarPorId(Long id) {
        // Questão anulada não é "encontrada": a query filtra, e o resultado é
        // 404. Do ponto de vista do produto ela não existe pra montar simulado.
        return questaoRepository.buscarComAlternativas(id)
                .orElseThrow(() -> new QuestaoNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<Integer> anosDisponiveis() {
        return questaoRepository.buscarAnosDisponiveis();
    }
}
