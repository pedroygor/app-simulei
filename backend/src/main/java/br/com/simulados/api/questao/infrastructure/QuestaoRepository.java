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

import java.util.Collection;
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
     * `q.disponivel = true` não é filtro do usuário: questão anulada ou incompleta
     * nunca entra em simulado, então nem aparece na busca. `disponivel` é coluna
     * gerada pelo banco (V9: NOT anulada AND NOT incompleta) — a regra mora num
     * lugar só. O índice parcial `(area, ano) WHERE disponivel` cobre este caminho.
     *
     * A prévia é cortada no BANCO (substring), não em Java: o texto grande nem
     * trafega. Coalesce porque 5% das questões não têm enunciado — aí a prévia
     * sai do comando.
     *
     * Matéria e assunto vêm da taxonomia por LEFT JOIN, no mesmo SELECT —
     * nada de buscar o assunto de cada questão depois (N+1). É LEFT e não
     * INNER porque questão ainda não classificada precisa continuar aparecendo.
     *
     * `:materia` e `:assunto` são SLUGS, minúsculos por construção (V5) e
     * validados por @Pattern no filtro — chegam aqui prontos pra comparar.
     * Nunca envolver o PARÂMETRO num lower(): quando ele é null, o driver o
     * manda como bytea e o Postgres responde "function lower(bytea) does not
     * exist" (bug achado no passo 4 da Etapa 2).
     */
    @Query(value = """
            SELECT new br.com.simulados.api.questao.domain.ResumoDeQuestao(
                       q.id, q.ano, q.numero, q.area, q.idioma, m.slug, m.nome, a.slug, a.nome,
                       substring(coalesce(q.enunciado, q.comando), 1, 300))
            FROM Questao q
            LEFT JOIN q.assunto a
            LEFT JOIN a.materia m
            WHERE q.disponivel = true
              AND (:area IS NULL OR q.area = :area)
              AND (:idioma IS NULL OR q.idioma = :idioma)
              AND (:anoInicial IS NULL OR q.ano >= :anoInicial)
              AND (:anoFinal IS NULL OR q.ano <= :anoFinal)
              AND (:materia IS NULL OR m.slug = :materia)
              AND (:assunto IS NULL OR a.slug = :assunto)
            """,
            // countQuery explícita: sem ela o Spring tentaria derivar o count a
            // partir do SELECT com construtor e falharia. Os mesmos joins, senão
            // o total não bate com a página quando há filtro por matéria
            countQuery = """
            SELECT count(q) FROM Questao q
            LEFT JOIN q.assunto a
            LEFT JOIN a.materia m
            WHERE q.disponivel = true
              AND (:area IS NULL OR q.area = :area)
              AND (:idioma IS NULL OR q.idioma = :idioma)
              AND (:anoInicial IS NULL OR q.ano >= :anoInicial)
              AND (:anoFinal IS NULL OR q.ano <= :anoFinal)
              AND (:materia IS NULL OR m.slug = :materia)
              AND (:assunto IS NULL OR a.slug = :assunto)
            """)
    Page<ResumoDeQuestao> buscar(@Param("area") Area area,
                                 @Param("idioma") Idioma idioma,
                                 @Param("anoInicial") Integer anoInicial,
                                 @Param("anoFinal") Integer anoFinal,
                                 @Param("materia") String materia,
                                 @Param("assunto") String assunto,
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
     *
     * A habilidade oficial e a competência dela também são "pra um" e LAZY: mesmo
     * fetch, mesma query. LEFT porque 5% das questões não têm habilidade publicada.
     */
    @Query("""
            SELECT q FROM Questao q
            LEFT JOIN FETCH q.alternativas
            LEFT JOIN FETCH q.assunto a
            LEFT JOIN FETCH a.materia
            LEFT JOIN FETCH q.habilidadeOficial h
            LEFT JOIN FETCH h.competencia
            WHERE q.id = :id AND q.disponivel = true
            """)
    Optional<Questao> buscarComAlternativas(@Param("id") Long id);

    /**
     * Várias questões com as alternativas, numa query só — a prova inteira de uma
     * tentativa (até 180). O Hibernate 6+ já elimina as linhas repetidas que o
     * JOIN FETCH da coleção produz, sem precisar de DISTINCT.
     *
     * Só as `disponivel` (decidido em 02/10/2026): questão anulada ou marcada
     * incompleta DEPOIS da montagem some da prova, que encolhe e é renumerada. Não
     * é trocada por outra — mudar a prova de quem já começou seria pior.
     */
    @Query("""
            SELECT q FROM Questao q
            LEFT JOIN FETCH q.alternativas
            WHERE q.id IN :ids AND q.disponivel = true
            """)
    List<Questao> buscarVariasComAlternativas(@Param("ids") Collection<Long> ids);

    /**
     * As mesmas questões, com assunto e matéria no mesmo fetch: a revisão do
     * resultado (3c) mostra o assunto de cada questão, e sem o fetch cada
     * `getAssunto().getNome()` dispararia um SELECT — N+1 escondido num getter.
     * Na prova não se busca isto: lá o assunto daria dica e nem vai na resposta.
     */
    @Query("""
            SELECT q FROM Questao q
            LEFT JOIN FETCH q.alternativas
            LEFT JOIN FETCH q.assunto a
            LEFT JOIN FETCH a.materia
            WHERE q.id IN :ids AND q.disponivel = true
            """)
    List<Questao> buscarVariasParaResultado(@Param("ids") Collection<Long> ids);

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
     * `disponivel` pelo mesmo motivo da busca: um ano cujas questões fossem
     * todas anuladas ou incompletas levaria o usuário a uma lista vazia.
     */
    @Query("SELECT DISTINCT q.ano FROM Questao q WHERE q.disponivel = true ORDER BY q.ano DESC")
    List<Integer> buscarAnosDisponiveis();

    /**
     * Todas as questões, pras cargas de arquivo (classificação por assunto e
     * conferência com o INEP) casarem cada linha do CSV em memória — UMA query
     * no lugar de uma busca por linha (2.741).
     *
     * Exceção consciente à regra "nunca findAll sem limite" (seção 8.4): é um
     * job de linha de comando sobre um acervo limitado por construção (~185
     * questões por prova), não uma listagem de usuário. Carrega entidade, e não
     * projeção, porque as cargas ALTERAM as questões pelo dirty checking.
     *
     * Sem JOIN FETCH do assunto de propósito: a classificação só compara o id do
     * assunto atual, e ler o id de um proxy lazy não dispara SELECT.
     *
     * Inclui as anuladas: a conferência com o INEP precisa enxergá-las pra ser
     * idempotente (uma questão já anulada é "inalterada", não "sem questão").
     */
    @Query("SELECT q FROM Questao q")
    List<Questao> buscarTodasParaCarga();
}
