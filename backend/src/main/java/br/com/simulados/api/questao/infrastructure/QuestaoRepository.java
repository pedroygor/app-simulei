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
     * Matéria e assunto vêm da taxonomia por LEFT JOIN, no mesmo SELECT —
     * nada de buscar o assunto de cada questão depois (N+1). É LEFT e não
     * INNER porque questão ainda não classificada precisa continuar aparecendo.
     *
     * `:materia` é o SLUG da matéria, que já é minúsculo por construção (V5).
     * Nunca envolver o PARÂMETRO num lower(): quando ele é null, o driver o
     * manda como bytea e o Postgres responde "function lower(bytea) does not
     * exist" (bug achado no passo 4 da Etapa 2).
     */
    @Query(value = """
            SELECT new br.com.simulados.api.questao.domain.ResumoDeQuestao(
                       q.id, q.ano, q.numero, q.area, q.idioma, m.nome, a.nome,
                       substring(coalesce(q.enunciado, q.comando), 1, 300))
            FROM Questao q
            LEFT JOIN q.assunto a
            LEFT JOIN a.materia m
            WHERE q.anulada = false
              AND (:area IS NULL OR q.area = :area)
              AND (:idioma IS NULL OR q.idioma = :idioma)
              AND (:anoInicial IS NULL OR q.ano >= :anoInicial)
              AND (:anoFinal IS NULL OR q.ano <= :anoFinal)
              AND (:materia IS NULL OR m.slug = :materia)
            """,
            // countQuery explícita: sem ela o Spring tentaria derivar o count a
            // partir do SELECT com construtor e falharia. Os mesmos joins, senão
            // o total não bate com a página quando há filtro por matéria
            countQuery = """
            SELECT count(q) FROM Questao q
            LEFT JOIN q.assunto a
            LEFT JOIN a.materia m
            WHERE q.anulada = false
              AND (:area IS NULL OR q.area = :area)
              AND (:idioma IS NULL OR q.idioma = :idioma)
              AND (:anoInicial IS NULL OR q.ano >= :anoInicial)
              AND (:anoFinal IS NULL OR q.ano <= :anoFinal)
              AND (:materia IS NULL OR m.slug = :materia)
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
     *
     * Assunto e matéria entram no mesmo fetch pelo mesmo motivo: são LAZY, e o
     * mapper toca nos dois pra montar a resposta. Dá pra buscar vários
     * relacionamentos "pra um" junto com UMA coleção; duas coleções (bags) é
     * que o Hibernate recusa (MultipleBagFetchException).
     */
    @Query("""
            SELECT q FROM Questao q
            LEFT JOIN FETCH q.alternativas
            LEFT JOIN FETCH q.assunto a
            LEFT JOIN FETCH a.materia
            WHERE q.id = :id AND q.anulada = false
            """)
    Optional<Questao> buscarComAlternativas(@Param("id") Long id);

    /**
     * Anos que têm ao menos uma questão utilizável, do mais recente pro mais
     * antigo. Alimenta o filtro de ano da tela.
     *
     * Existe pra que o intervalo oferecido ao usuário venha dos DADOS, não de
     * uma constante: o front tinha 2009–2030 fixo e oferecia 2024–2030, que
     * não existem nem na fonte. Quando a importação trouxer uma prova nova, o
     * ano aparece sozinho.
     *
     * Projeção escalar: o JPQL devolve só a coluna e o Spring Data mapeia
     * direto pra List<Integer> — nenhuma entidade é carregada.
     *
     * Sem Pageable, e é exceção consciente à regra "toda listagem é paginada"
     * (seção 8.4): o resultado é limitado POR CONSTRUÇÃO, uma linha por prova
     * do ENEM (~15 hoje, uma a mais por ano).
     *
     * `anulada = false` pelo mesmo motivo da busca: um ano cujas questões
     * fossem todas anuladas levaria o usuário a uma lista vazia.
     */
    @Query("SELECT DISTINCT q.ano FROM Questao q WHERE q.anulada = false ORDER BY q.ano DESC")
    List<Integer> buscarAnosDisponiveis();
}
