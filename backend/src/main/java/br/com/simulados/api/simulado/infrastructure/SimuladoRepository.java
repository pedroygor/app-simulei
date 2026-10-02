package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.simulado.domain.Simulado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SimuladoRepository extends JpaRepository<Simulado, UUID> {
}
