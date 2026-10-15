package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.QuestaoDoSimulado;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real: o que se testa é o mapeamento do @ElementCollection contra a V10
// (ordem, PK composta, FKs) — nada disso existe num mock
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SimuladoRepositoryTest {

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID dono;
    private Long q1;
    private Long q2;
    private Long q3;

    @BeforeEach
    void setUp() {
        var usuario = new Usuario();
        usuario.setNome("Professora");
        usuario.setEmail("professora@exemplo.com");
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        dono = usuarioRepository.save(usuario).getId();
        q1 = questaoRepository.save(questao(1)).getId();
        q2 = questaoRepository.save(questao(2)).getId();
        q3 = questaoRepository.save(questao(3)).getId();
        sincronizar();
    }

    @Test
    void guardaAsQuestoesNaOrdemRecebidaENaoNaDeInsercao() {
        // A prova pede q3, q1, q2: a ordem é a da lista, não a dos ids
        var id = simuladoRepository.save(new Simulado(dono, "Lista de probabilidade", 3600, List.of(q3, q1, q2))).getId();
        sincronizar();

        var relido = simuladoRepository.findById(id).orElseThrow();

        assertThat(relido.getQuestoes()).containsExactly(
                new QuestaoDoSimulado(q3, (short) 1),
                new QuestaoDoSimulado(q1, (short) 2),
                new QuestaoDoSimulado(q2, (short) 3));
        assertThat(relido.getCriadoEm()).isNotNull();
        assertThat(relido.pertenceA(dono)).isTrue();
        assertThat(relido.pertenceA(UUID.randomUUID())).isFalse();
    }

    @Test
    void semLimiteDeTempoEhNull() {
        var id = simuladoRepository.save(new Simulado(dono, "Treino", null, List.of(q1))).getId();
        sincronizar();

        assertThat(simuladoRepository.findById(id).orElseThrow().getTempoLimiteSeg()).isNull();
    }

    @Test
    void dominioRecusaQuestaoRepetidaEListaVazia() {
        assertThatThrownBy(() -> new Simulado(dono, "x", null, List.of(q1, q1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Simulado(dono, "x", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bancoRecusaQuestaoInexistente() {
        simuladoRepository.save(new Simulado(dono, "x", null, List.of(q1, 999_999L)));

        // ConstraintViolationException do Hibernate, não a DataIntegrityViolation do
        // Spring: o flush() direto no EntityManager não passa pela tradução de
        // exceções, que o Spring só aplica nos métodos do repository
        assertThatThrownBy(this::sincronizar)
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("simulado_questoes_questao_id_fkey");
    }

    @Test
    void bancoRecusaTempoForaDoIntervalo() {
        simuladoRepository.save(new Simulado(dono, "x", 30, List.of(q1)));

        assertThatThrownBy(this::sincronizar)
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_simulados_tempo");
    }

    // --- apoio ---

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Questao questao(int numero) {
        var questao = new Questao();
        questao.setAno(2020);
        questao.setNumero(numero + 135);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2020/" + (numero + 135));
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
