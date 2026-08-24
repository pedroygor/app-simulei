package br.com.simulados.api.usuario.infrastructure;

import br.com.simulados.api.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    // O Spring Data gera a query a partir do nome do método:
    // SELECT EXISTS(... WHERE email = ?) — sem trazer a entidade inteira
    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}
