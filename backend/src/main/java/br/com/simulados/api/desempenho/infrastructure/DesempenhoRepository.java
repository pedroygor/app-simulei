package br.com.simulados.api.desempenho.infrastructure;

import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import br.com.simulados.api.simulado.domain.Tentativa;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Leitura agregada do desempenho do aluno.
 *
 * `Repository` (e não `JpaRepository`): a interface marcadora do Spring Data não traz
 * save/findAll/delete — só os métodos declarados aqui. Este repositório só LÊ
 * agregados; não faz sentido ele oferecer escrita de tentativa (isso é do
 * TentativaRepository).
 */
public interface DesempenhoRepository extends Repository<Tentativa, UUID> {

    /**
     * O mesmo GROUP BY do resultado da prova (TentativaRepository.placarPorAssunto),
     * sobre TODAS as provas entregues do aluno — um SELECT só, qualquer que seja o
     * número de provas (seção 8.4).
     *
     * - Só FINALIZADA: prova aberta está pela metade e, somada aqui, revelaria o
     *   gabarito (responde, olha o número mudar)
     * - `q.disponivel`: questão anulada ou incompleta depois da prova sai também do
     *   diagnóstico, como já sai da nota
     * - O assunto vem de `q.assunto`, a classificação de HOJE: reclassificar uma
     *   questão corrige o histórico sem recalcular nada
     * - A mesma questão em duas provas conta duas vezes: cada resposta é uma observação
     * - O filtro de dono é `t.alunoId`, que vem do token, nunca do cliente
     */
    @Query("""
            SELECT new br.com.simulados.api.simulado.domain.LinhaDoResultado(
                       q.area, m.slug, m.nome, a.slug, a.nome,
                       count(q),
                       count(r.questaoId),
                       sum(CASE WHEN r.correta = true THEN 1 ELSE 0 END))
            FROM Tentativa t
            JOIN Simulado s ON s.id = t.simuladoId
            JOIN s.questoes sq
            JOIN Questao q ON q.id = sq.questaoId
            LEFT JOIN q.assunto a
            LEFT JOIN a.materia m
            LEFT JOIN Resposta r ON r.tentativaId = t.id AND r.questaoId = q.id
            WHERE t.alunoId = :aluno
              AND t.status = br.com.simulados.api.simulado.domain.StatusDaTentativa.FINALIZADA
              AND q.disponivel = true
            GROUP BY q.area, m.slug, m.nome, m.ordem, a.slug, a.nome, a.ordem
            ORDER BY m.ordem, a.ordem
            """)
    List<LinhaDoResultado> linhasDoAluno(@Param("aluno") UUID aluno);

    /** Quantas provas entraram na conta ("com base em N provas"). */
    @Query("""
            SELECT count(t) FROM Tentativa t
            WHERE t.alunoId = :aluno
              AND t.status = br.com.simulados.api.simulado.domain.StatusDaTentativa.FINALIZADA
            """)
    long provasEntregues(@Param("aluno") UUID aluno);

    /**
     * A evolução por semana (4c): o mesmo recorte de linhasDoAluno (só entregues, só
     * `disponivel`, classificação de hoje), agrupado por SEMANA e área, a partir de
     * `desde`. Um SELECT só pra janela inteira.
     *
     * SQL nativo porque o JPQL não tem date_trunc com fuso. A semana é a de
     * Brasília, segunda a domingo: `finalizado_em` é TIMESTAMPTZ, e sem o AT TIME
     * ZONE uma prova entregue domingo às 22h (já segunda em UTC) cairia na semana
     * seguinte. A prova conta na semana em que foi ENTREGUE.
     *
     * `assunto` (id) opcional: com ele, a evolução de UM assunto. O CAST dá tipo ao
     * parâmetro nulo — sem ele o driver manda null sem tipo e o Postgres recusa
     * (a mesma armadilha do lower(:param) da busca, na Etapa 2).
     */
    @Query(value = """
            SELECT CAST(date_trunc('week', t.finalizado_em AT TIME ZONE 'America/Sao_Paulo') AS date) AS semana,
                   q.area AS area,
                   count(*) AS questoes,
                   count(r.questao_id) AS respondidas,
                   count(*) FILTER (WHERE r.correta) AS acertos
            FROM tentativas t
            JOIN simulado_questoes sq ON sq.simulado_id = t.simulado_id
            JOIN questoes q ON q.id = sq.questao_id
            LEFT JOIN respostas r ON r.tentativa_id = t.id AND r.questao_id = q.id
            WHERE t.aluno_id = :aluno
              AND t.status = 'FINALIZADA'
              AND t.finalizado_em >= :desde
              AND q.disponivel
              AND (CAST(:assunto AS integer) IS NULL OR q.assunto_id = :assunto)
            GROUP BY 1, 2
            ORDER BY 1, 2
            """, nativeQuery = true)
    List<LinhaDaSemana> semanasDoAluno(@Param("aluno") UUID aluno, @Param("desde") Instant desde,
                                       @Param("assunto") Integer assunto);
}
