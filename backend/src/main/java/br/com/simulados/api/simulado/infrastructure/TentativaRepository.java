package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.simulado.domain.Tentativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TentativaRepository extends JpaRepository<Tentativa, UUID> {

    /**
     * A tentativa SÓ se for do aluno: autorização por dono na própria query, como
     * em SimuladoRepository.buscarDoUsuario. Query derivada do nome do método:
     * o Spring Data gera "WHERE t.id = ?1 AND t.alunoId = ?2".
     */
    Optional<Tentativa> findByIdAndAlunoId(UUID id, UUID alunoId);

    // A tentativa do aluno naquele simulado, se já existir (iniciar idempotente)
    Optional<Tentativa> findBySimuladoIdAndAlunoId(UUID simuladoId, UUID alunoId);
}
