package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.simulado.domain.LinhaDoResultado;
import br.com.simulados.api.simulado.domain.Tentativa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TentativaRepository extends JpaRepository<Tentativa, UUID> {

    /**
     * A tentativa SÓ se for do aluno: autorização por dono na própria query, como
     * em SimuladoRepository.buscarDoUsuario. Query derivada do nome do método:
     * o Spring Data gera "WHERE t.id = ?1 AND t.alunoId = ?2".
     */
    Optional<Tentativa> findByIdAndAlunoId(UUID id, UUID alunoId);

    /**
     * A mesma busca, travando a linha até o fim da transação (@Lock vira
     * SELECT ... FOR UPDATE no Postgres). É pra quem vai ESCREVER na tentativa:
     * sem a trava, um "responder" que leu EM_ANDAMENTO gravaria a resposta depois
     * de um "finalizar" de outra aba já ter entregado a prova. Com ela, um espera o
     * outro terminar e enxerga o status novo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tentativa t WHERE t.id = :id AND t.alunoId = :aluno")
    Optional<Tentativa> buscarDoAlunoParaEscrita(@Param("id") UUID id, @Param("aluno") UUID aluno);

    Optional<Tentativa> findBySimuladoIdAndAlunoId(UUID simuladoId, UUID alunoId);

    /**
     * O placar da tentativa por assunto, num GROUP BY só (seção 8.4). A base pro
     * desempenho da Etapa 4, que é o mesmo cálculo sobre várias tentativas.
     *
     * - Parte das QUESTÕES do simulado, não das respostas: o LEFT JOIN com
     *   Resposta faz a questão em branco entrar, com r nulo. count(r.questaoId)
     *   conta só as respondidas (count de coluna ignora nulo)
     * - `q.disponivel` no WHERE: anulada ou incompleta depois da montagem não
     *   conta, nem a resposta dada a ela antes (ADR de 02/10/2026)
     * - LEFT JOIN no assunto: questão ainda sem classificação vira uma linha com
     *   assunto nulo, em vez de sumir da nota
     * - `correta` vem da resposta, gravada na hora (seção 6), sem juntar gabarito
     *
     * Ordem da taxonomia; o "pior primeiro" é regra de apresentação e fica no service.
     * Sem filtro de dono: quem chama já autorizou pela tentativa.
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
            WHERE t.id = :tentativa AND q.disponivel = true
            GROUP BY q.area, m.slug, m.nome, m.ordem, a.slug, a.nome, a.ordem
            ORDER BY m.ordem, a.ordem
            """)
    List<LinhaDoResultado> placarPorAssunto(@Param("tentativa") UUID tentativa);

    /**
     * Cria a tentativa se ainda não existir, numa instrução só. Por que não "busca e,
     * se não achar, save()": dois toques no "Iniciar" chegam juntos, os dois não
     * acham nada, e o segundo INSERT bate no UNIQUE (simulado, aluno) — o aluno
     * levaria um erro por ter clicado duas vezes. ON CONFLICT DO NOTHING deixa o
     * segundo passar em silêncio.
     *
     * O SELECT ... WHERE criado_por faz a autorização no próprio INSERT: simulado
     * alheio ou inexistente não insere nada (Etapa 5: aqui entra "ou da turma").
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO tentativas (simulado_id, aluno_id, status, prazo)
            SELECT s.id, :aluno, 'EM_ANDAMENTO',
                   -- now() é o MESMO instante do iniciado_em (default da coluna):
                   -- dentro de uma transação o now() do Postgres não anda.
                   -- Sem tempo-limite, make_interval(NULL) dá NULL = sem prazo
                   now() + make_interval(secs => s.tempo_limite_seg)
            FROM simulados s
            WHERE s.id = :simulado AND s.criado_por = :aluno
            ON CONFLICT (simulado_id, aluno_id) DO NOTHING
            """, nativeQuery = true)
    int criarSeNaoExistir(@Param("simulado") UUID simulado, @Param("aluno") UUID aluno);

    /**
     * Entrega as provas abandonadas com o tempo vencido (o aluno fechou o app).
     * Um UPDATE só, qualquer que seja o número de provas — pelo índice parcial da
     * V12. `finalizado_em = prazo`: a prova acabou no prazo, não quando o job passou.
     *
     * `limite` = agora − tolerância (calculado em Java, com o Clock injetado): a
     * resposta tocada no último segundo ainda pode estar chegando, e o responder
     * a aceita até o prazo + tolerância.
     *
     * Concorrência: o UPDATE trava cada linha que muda. Um responder que tenha
     * pego o FOR UPDATE antes termina primeiro; um que chegue depois vê FINALIZADA.
     */
    @Modifying
    @Query(value = """
            UPDATE tentativas
            SET status = 'FINALIZADA', finalizado_em = prazo
            WHERE status = 'EM_ANDAMENTO' AND prazo IS NOT NULL AND prazo <= :limite
            """, nativeQuery = true)
    int encerrarVencidas(@Param("limite") Instant limite);
}
