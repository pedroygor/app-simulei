package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.application.BuscaDeQuestoes;
import br.com.simulados.api.shared.exception.OrdenacaoInvalidaException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/questoes")
@RequiredArgsConstructor
public class QuestaoController {

    /**
     * Só estes campos podem ir no ?sort=. Whitelist e não lista negra: com
     * @Query explícita o Spring Data concatena a ordenação no JPQL sem validar
     * nada, e um campo inexistente estoura lá no Hibernate como 500. Além
     * disso, limitar a superfície é bom hábito — ordenar por campo interno não
     * é funcionalidade que alguém pediu.
     */
    private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("ano", "numero", "area", "idioma", "id");

    private final BuscaDeQuestoes buscaDeQuestoes;
    private final QuestaoMapper questaoMapper;

    /**
     * Listagem paginada. Exige autenticação (regra geral do SecurityConfig):
     * ALUNO e PROFESSOR podem buscar questões.
     *
     * O record de filtro chega por query param — o Spring monta o objeto a
     * partir de ?area=MATEMATICA&anoInicial=2020, e o @Valid dispara o Bean
     * Validation nele igual faria num corpo de request.
     */
    @GetMapping
    public Page<QuestaoResumoResponse> buscar(
            @Valid BuscarQuestoesFiltro filtro,
            // Ordenação fixa por padrão: prova mais nova primeiro, questão em
            // ordem. Sem isso a ordem entre páginas não é garantida pelo banco.
            @PageableDefault(size = 20, sort = {"ano", "numero"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        validarOrdenacao(pageable);
        return buscaDeQuestoes
                .buscar(filtro.area(), filtro.idioma(), filtro.menorAno(), filtro.maiorAno(),
                        filtro.materiaNormalizada(), pageable)
                .map(questaoMapper::toResumo);
    }

    private void validarOrdenacao(Pageable pageable) {
        var invalidos = pageable.getSort().stream()
                .map(Sort.Order::getProperty)
                .filter(campo -> !CAMPOS_ORDENAVEIS.contains(campo))
                .toList();
        if (!invalidos.isEmpty()) {
            throw new OrdenacaoInvalidaException(invalidos, CAMPOS_ORDENAVEIS);
        }
    }

    @GetMapping("/{id}")
    public QuestaoDetalheResponse porId(@PathVariable Long id) {
        return questaoMapper.toDetalhe(buscaDeQuestoes.buscarPorId(id));
    }
}
