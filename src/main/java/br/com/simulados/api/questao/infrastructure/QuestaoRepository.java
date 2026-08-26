package br.com.simulados.api.questao.infrastructure;

import br.com.simulados.api.questao.domain.ChaveNaturalQuestao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
