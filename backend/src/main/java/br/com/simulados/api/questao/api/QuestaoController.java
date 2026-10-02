package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.application.BuscaDeQuestoes;
import br.com.simulados.api.shared.exception.OrdenacaoInvalidaException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
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
     * Detalhe da questão. O gabarito só vai pro PROFESSOR: o aluno vê o gabarito
     * apenas no resultado da prova, depois de finalizar (decidido em 01/10/2026).
     * Sem isto, durante uma prova ele abriria a questão em outra aba e leria a resposta.
     *
     * `Authentication`: o Spring injeta o usuário autenticado no parâmetro, já com
     * as authorities que o SecurityConfig montou a partir da claim "role" do JWT.
     * A regra é por omissão: quem não é professor (inclusive um papel novo no
     * futuro) não recebe o gabarito.
     */
    @GetMapping("/{id}")
    public QuestaoDetalheResponse porId(@PathVariable Long id, Authentication autenticacao) {
        var detalhe = questaoMapper.toDetalhe(buscaDeQuestoes.buscarPorId(id));
        return ehProfessor(autenticacao) ? detalhe : detalhe.semGabarito();
    }

    private static boolean ehProfessor(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PROFESSOR".equals(authority.getAuthority()));
    }
}
