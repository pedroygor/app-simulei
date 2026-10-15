package br.com.simulados.api.desempenho.infrastructure;

import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import br.com.simulados.api.simulado.domain.Tentativa;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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
}
