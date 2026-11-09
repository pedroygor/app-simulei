package br.com.simulados.api.usuario.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
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

    // O CHECK da V14: e-mail com maiúscula não entra nem por fora da API
    @Test
    void bancoRecusaEmailComMaiuscula() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO usuarios (id, nome, email, senha_hash, role)
                VALUES (gen_random_uuid(), 'Ana', 'Ana@teste.com', 'hash', 'ALUNO')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuarios_email_minusculo");
    }

    // V15: conta só com Google existe, sem senha
    @Test
    void bancoAceitaContaSoComGoogle() {
        var linhas = jdbc.update("""
                INSERT INTO usuarios (id, nome, email, senha_hash, google_sub, role)
                VALUES (gen_random_uuid(), 'Ana', 'ana.google@teste.com', NULL, 'sub-ana', 'ALUNO')
                """);

        assertThat(linhas).isEqualTo(1);
    }

    // V15: nenhuma conta fica sem jeito de entrar
    @Test
    void bancoRecusaContaSemSenhaNemGoogle() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO usuarios (id, nome, email, senha_hash, google_sub, role)
                VALUES (gen_random_uuid(), 'Ninguém', 'sem.acesso@teste.com', NULL, NULL, 'ALUNO')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuarios_forma_de_entrar");
    }

    // V15: uma conta Google liga a uma conta Simulei só
    @Test
    void bancoRecusaAMesmaContaGoogleEmDuasContas() {
        jdbc.update("""
                INSERT INTO usuarios (id, nome, email, senha_hash, google_sub, role)
                VALUES (gen_random_uuid(), 'Bia', 'bia.um@teste.com', NULL, 'sub-bia', 'ALUNO')
                """);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO usuarios (id, nome, email, senha_hash, google_sub, role)
                VALUES (gen_random_uuid(), 'Bia', 'bia.dois@teste.com', NULL, 'sub-bia', 'ALUNO')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_usuarios_google_sub");
    }
}
