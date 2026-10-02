package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.simulado.domain.Tentativa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            INSERT INTO tentativas (simulado_id, aluno_id, status)
            SELECT s.id, :aluno, 'EM_ANDAMENTO'
            FROM simulados s
            WHERE s.id = :simulado AND s.criado_por = :aluno
            ON CONFLICT (simulado_id, aluno_id) DO NOTHING
            """, nativeQuery = true)
    int criarSeNaoExistir(@Param("simulado") UUID simulado, @Param("aluno") UUID aluno);
}
