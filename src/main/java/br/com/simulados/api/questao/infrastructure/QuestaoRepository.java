package br.com.simulados.api.questao.infrastructure;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.ChaveNaturalQuestao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.domain.ResumoDeQuestao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

    // Chave natural da fonte — o job de importação usa pra ser idempotente
    // (rodar duas vezes não duplica questão)
    boolean existsByAnoAndNumeroAndIdioma(Integer ano, Integer numero, Idioma idioma);

    /**
     * Traz as chaves já importadas de um ano numa ÚNICA query.
     *
     * O caminho ingênuo seria chamar existsByAnoAndNumeroAndIdioma dentro do
     * laço de importação: 185 SELECTs por ano, ~2.800 no total. Aqui é 1 por
     * ano, e a comparação acontece em memória (seção 8.4 do CLAUDE.md).
     *
     * A projeção pra record evita carregar a entidade inteira — só duas colunas.
     */
    @Query("SELECT new br.com.simulados.api.questao.domain.ChaveNaturalQuestao(q.numero, q.idioma) "
            + "FROM Questao q WHERE q.ano = :ano")
    List<ChaveNaturalQuestao> buscarChavesDoAno(@Param("ano") int ano);

    /**
     * Busca paginada com filtros opcionais.
     *
     * O padrão `:param IS NULL OR coluna = :param` resolve filtro dinâmico numa
     * query só, sem Criteria API nem concatenação de string. Parâmetro nulo
     * simplesmente não filtra.
     *
     * `q.anulada = false` não é filtro do usuário: questão anulada nunca entra
     * em simulado, então nem aparece na busca (regra da seção 4 do CLAUDE.md).
     * O índice parcial `(area, ano) WHERE NOT anulada` da V2 cobre exatamente
     * este caminho.
     *
     * A prévia é cortada no BANCO (substring), não em Java: o texto grande nem
     * trafega. Coalesce porque 5% das questões não têm enunciado — aí a prévia
     * sai do comando.
     *
     * Sobre `lower(q.materia) = :materia`: a matéria já chega em minúsculas de
     * quem chama. Envolver o PARÂMETRO num lower() quebra no Postgres quando
     * ele é null — sem contexto de tipo, o driver manda o null como bytea e o
     * banco responde "function lower(bytea) does not exist".
     */
    @Query(value = """
            SELECT new br.com.simulados.api.questao.domain.ResumoDeQuestao(
                       q.id, q.ano, q.numero, q.area, q.idioma, q.materia, q.assunto,
                       substring(coalesce(q.enunciado, q.comando), 1, 300))
            FROM Questao q
            WHERE q.anulada = false
              AND (:area IS NULL OR q.area = :area)
              AND (:idioma IS NULL OR q.idioma = :idioma)
              AND (:anoInicial IS NULL OR q.ano >= :anoInicial)
              AND (:anoFinal IS NULL OR q.ano <= :anoFinal)
              AND (:materia IS NULL OR lower(q.materia) = :materia)
            """,
            // countQuery explícita: sem ela o Spring tentaria derivar o count a
            // partir do SELECT com construtor e falharia
            countQuery = """
            SELECT count(q) FROM Questao q
            WHERE q.anulada = false
              AND (:area IS NULL OR q.area = :area)
              AND (:idioma IS NULL OR q.idioma = :idioma)
              AND (:anoInicial IS NULL OR q.ano >= :anoInicial)
              AND (:anoFinal IS NULL OR q.ano <= :anoFinal)
              AND (:materia IS NULL OR lower(q.materia) = :materia)
            """)
    Page<ResumoDeQuestao> buscar(@Param("area") Area area,
                                 @Param("idioma") Idioma idioma,
                                 @Param("anoInicial") Integer anoInicial,
                                 @Param("anoFinal") Integer anoFinal,
                                 @Param("materia") String materia,
                                 Pageable pageable);

    /**
     * Questão completa com as alternativas em UMA query.
     *
     * Sem o JOIN FETCH seriam duas: uma pra questão e outra disparada quando o
     * mapper tocasse na lista lazy. Com uma questão só a diferença é pequena,
     * mas o hábito é o que evita o N+1 quando isso virar laço.
     */
    @Query("""
            SELECT q FROM Questao q
            LEFT JOIN FETCH q.alternativas
            WHERE q.id = :id AND q.anulada = false
            """)
    Optional<Questao> buscarComAlternativas(@Param("id") Long id);
}
