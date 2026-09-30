package br.com.simulados.api.questao.infrastructure;

import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.LinhaDaTaxonomia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssuntoRepository extends JpaRepository<Assunto, Integer> {

    /**
     * A taxonomia inteira com a contagem de questões por assunto, em UMA query.
     *
     * `LEFT JOIN Questao q ON q.assunto = a` é um join SEM relacionamento
     * mapeado — o JPQL do Hibernate 6+ aceita. É de propósito: mapear um
     * @OneToMany de Assunto pra Questao só pra contar deixaria à mão um
     * `assunto.getQuestoes()` que carrega milhares de questões por engano.
     *
     * `q.anulada = false` fica no ON, e NÃO no WHERE. No WHERE, a condição
     * seria avaliada depois do join e descartaria as linhas sem questão (onde
     * `anulada` é nulo) — os assuntos com zero questões sumiriam da lista. No
     * ON, ela só decide quais questões entram na contagem.
     *
     * Ordem pelas colunas `ordem` (V7): a sequência de estudo do
     * docs/taxonomia.md. Nunca por id — o id é identity e reflete a ordem em
     * que o plano do INSERT da V5 produziu as linhas, que saiu invertida.
     * Áreas aparecem na ordem da primeira matéria de cada uma.
     */
    @Query("""
            SELECT new br.com.simulados.api.questao.domain.LinhaDaTaxonomia(
                       m.area, m.slug, m.nome, a.slug, a.nome, count(q))
            FROM Assunto a
            JOIN a.materia m
            LEFT JOIN Questao q ON q.assunto = a AND q.anulada = false
            GROUP BY m.id, m.area, m.slug, m.nome, m.ordem, a.id, a.slug, a.nome, a.ordem
            ORDER BY m.ordem, a.ordem
            """)
    List<LinhaDaTaxonomia> listarComContagem();
}
