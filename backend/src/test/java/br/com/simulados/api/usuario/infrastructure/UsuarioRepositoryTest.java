package br.com.simulados.api.usuario.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O CHECK da V13: professor não entra nem por fora da API (carga, script, SQL à mão).
 * JdbcTemplate e não a entidade, porque o enum Role já não tem PROFESSOR — o que se
 * testa aqui é o banco.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UsuarioRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void bancoRecusaPapelProfessor() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO usuarios (id, nome, email, senha_hash, role)
                VALUES (gen_random_uuid(), 'Prof', 'prof@teste.com', 'hash', 'PROFESSOR')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuarios_role");
    }
}
