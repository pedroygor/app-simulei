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

import java.util.List;
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
     * Qualquer usuário autenticado pode buscar questões.
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
                        filtro.materia(), filtro.assunto(), pageable)
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

    /**
     * Anos com questões disponíveis, do mais recente pro mais antigo.
     *
     * Não conflita com /{id} abaixo: as duas rotas casam com /questoes/anos,
     * mas o Spring MVC escolhe o padrão mais ESPECÍFICO — segmento literal
     * ganha de variável. Há teste garantindo isso, porque se a escolha
     * invertesse, "anos" cairia no {id} e viraria 400 (não é um Long).
     *
     * Sem parâmetro de entrada, não há o que validar.
     */
    @GetMapping("/anos")
    public List<Integer> anosDisponiveis() {
        return buscaDeQuestoes.anosDisponiveis();
    }

    /**
     * Detalhe da questão, COM gabarito (decidido em 15/10/2026). Antes só o
     * professor o recebia, pro aluno não abrir a questão em outra aba durante a
     * prova. Sem professor e com o Simulei como estudo pessoal, o banco de questões
     * serve pra treinar, e treinar sem poder conferir não serve. Quem lê o gabarito
     * durante a própria prova só falseia o próprio diagnóstico. A tela esconde a
     * resposta atrás de "Ver gabarito".
     *
     * A PROVA continua sem gabarito: ele só vem no resultado da tentativa entregue
     * (ResultadoResponse), e o teste da 3c garante isso.
     */
    @GetMapping("/{id}")
    public QuestaoDetalheResponse porId(@PathVariable Long id) {
        return questaoMapper.toDetalhe(buscaDeQuestoes.buscarPorId(id));
    }
}
