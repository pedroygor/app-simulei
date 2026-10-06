package br.com.simulados.api.simulado.infrastructure;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * As duas consultas da montagem de prova (3a). Cada escolha do pedido chega
 * aqui já EXPANDIDA num conjunto de ids de assunto (Matemática = os 23 assuntos
 * dela) e numerada como um GRUPO; os grupos nunca se sobrepõem (o service recusa).
 *
 * São sempre DUAS queries, com 1 escolha ou com 30: contar, e depois sortear tudo
 * de uma vez. Uma query por escolha é o N+1 da seção 8.4.
 *
 * Classe com EntityManager, e não interface do Spring Data, porque o sorteio é SQL
 * nativo (janela + unnest de arrays) e o resultado não é uma entidade.
 *
 * Ambas filtram `disponivel` (V9): anulada e incompleta nunca entram em prova.
 */
@Repository
@RequiredArgsConstructor
public class SorteadorDeQuestoes {

    private final EntityManager entityManager;

    /**
     * Quantas questões disponíveis há em cada assunto, nos anos pedidos (vazio =
     * todos). Assunto sem questão nenhuma simplesmente não aparece no mapa.
     */
    public Map<Integer, Long> contarPorAssunto(Set<Integer> assuntos, List<Integer> anos) {
        // `q.ano IN :anos` com lista vazia não é SQL válido em todo banco; o
        // parâmetro booleano decide, e a lista ganha um valor inócuo quando vazia
        var todosOsAnos = anos.isEmpty();
        var linhas = entityManager.createQuery("""
                        SELECT q.assunto.id, count(q) FROM Questao q
                        WHERE q.disponivel = true
                          AND q.assunto.id IN :assuntos
                          AND (:todosOsAnos = true OR q.ano IN :anos)
                        GROUP BY q.assunto.id
                        """, Object[].class)
                .setParameter("assuntos", assuntos)
                .setParameter("todosOsAnos", todosOsAnos)
                .setParameter("anos", todosOsAnos ? List.of(0) : anos)
                .getResultList();

        var contagem = new HashMap<Integer, Long>();
        for (var linha : linhas) {
            contagem.put((Integer) linha[0], (Long) linha[1]);
        }
        return contagem;
    }

    /**
     * Sorteia, numa query só, `quantidades[g]` questões de cada grupo g.
     *
     * `row_number() OVER (PARTITION BY grupo ORDER BY random())` numera as
     * candidatas de cada grupo numa ordem aleatória; ficam as de número ≤ a cota do
     * grupo. O pedido entra como arrays do Postgres (`unnest`) em vez de SQL
     * montado por concatenação — sem injeção, e com um plano só pra qualquer
     * tamanho de pedido.
     *
     * Volta os ids agrupados pela ordem das escolhas e, dentro do grupo, na ordem
     * do sorteio: a prova sai "Natureza, depois Matemática", como foi pedida.
     *
     * @param grupoDoAssunto assunto → nº do grupo (0..n-1), o da escolha que o contém
     * @param quantidades    cota de cada grupo, já calculada pelo DistribuidorDeQuestoes
     */
    public List<Long> sortear(Map<Integer, Integer> grupoDoAssunto, List<Integer> quantidades, List<Integer> anos) {
        var assuntos = new ArrayList<Integer>(grupoDoAssunto.keySet());
        var grupos = assuntos.stream().map(grupoDoAssunto::get).toList();

        @SuppressWarnings("unchecked")
        List<Number> ids = entityManager.createNativeQuery("""
                        WITH pedido AS (
                            SELECT * FROM unnest(CAST(:assuntos AS int[]), CAST(:grupos AS int[])) AS p(assunto_id, grupo)
                        ),
                        cota AS (
                            SELECT grupo - 1 AS grupo, quantidade
                            FROM unnest(CAST(:quantidades AS int[])) WITH ORDINALITY AS c(quantidade, grupo)
                        ),
                        candidatas AS (
                            SELECT q.id, p.grupo,
                                   row_number() OVER (PARTITION BY p.grupo ORDER BY random()) AS posicao
                            FROM questoes q
                            JOIN pedido p ON p.assunto_id = q.assunto_id
                            WHERE q.disponivel
                              AND (cardinality(CAST(:anos AS int[])) = 0 OR q.ano = ANY (CAST(:anos AS int[])))
                        )
                        SELECT c.id
                        FROM candidatas c
                        JOIN cota ON cota.grupo = c.grupo
                        WHERE c.posicao <= cota.quantidade
                        ORDER BY c.grupo, c.posicao
                        """)
                .setParameter("assuntos", assuntos.toArray(Integer[]::new))
                .setParameter("grupos", grupos.toArray(Integer[]::new))
                .setParameter("quantidades", quantidades.toArray(Integer[]::new))
                .setParameter("anos", anos.toArray(Integer[]::new))
                .getResultList();

        return ids.stream().map(Number::longValue).toList();
    }
}
