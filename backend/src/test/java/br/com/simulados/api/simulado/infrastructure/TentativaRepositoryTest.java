package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.Resposta;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.domain.StatusDaTentativa;
import br.com.simulados.api.simulado.domain.Tentativa;
import br.com.simulados.api.simulado.domain.TentativaJaFinalizadaException;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real: upsert com ON CONFLICT, FK composta e UNIQUE só existem no Postgres
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TentativaRepositoryTest {

    @Autowired
    private TentativaRepository tentativaRepository;

    @Autowired
    private RespostaRepository respostaRepository;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID aluno;
    private UUID simulado;
    private Questao q1;
    private Questao q2;

    @BeforeEach
    void setUp() {
        aluno = usuario("aluna@exemplo.com");
        q1 = questaoRepository.save(questao(1));
        q2 = questaoRepository.save(questao(2));
        simulado = simuladoRepository.save(new Simulado(aluno, "Treino", null, List.of(q1.getId(), q2.getId()))).getId();
        sincronizar();
    }

    @Test
    void nasceEmAndamentoESoApareceProDono() {
        var id = tentativaRepository.save(new Tentativa(simulado, aluno)).getId();
        sincronizar();

        var relida = tentativaRepository.findByIdAndAlunoId(id, aluno).orElseThrow();
        assertThat(relida.getStatus()).isEqualTo(StatusDaTentativa.EM_ANDAMENTO);
        assertThat(relida.getIniciadoEm()).isNotNull();
        assertThat(relida.getFinalizadoEm()).isNull();
        assertThat(tentativaRepository.findByIdAndAlunoId(id, usuario("outro@exemplo.com"))).isEmpty();
        assertThat(tentativaRepository.findBySimuladoIdAndAlunoId(simulado, aluno)).isPresent();
    }

    @Test
    void finalizarGravaAHoraEAProvaEntregueNaoMudaMais() {
        var tentativa = tentativaRepository.save(new Tentativa(simulado, aluno));
        var agora = Instant.parse("2026-10-02T12:00:00Z");

        tentativa.finalizar(agora);
        sincronizar();

        var relida = tentativaRepository.findById(tentativa.getId()).orElseThrow();
        assertThat(relida.getStatus()).isEqualTo(StatusDaTentativa.FINALIZADA);
        assertThat(relida.getFinalizadoEm()).isEqualTo(agora);
        assertThatThrownBy(() -> relida.finalizar(agora)).isInstanceOf(TentativaJaFinalizadaException.class);
        assertThatThrownBy(relida::exigirEmAndamento).isInstanceOf(TentativaJaFinalizadaException.class);
    }

    @Test
    void bancoRecusaSegundaTentativaDoMesmoAlunoNoMesmoSimulado() {
        tentativaRepository.save(new Tentativa(simulado, aluno));

        // saveAndFlush passa pela tradução de exceções do Spring (é método do
        // repository), por isso aqui chega a DataIntegrityViolation
        assertThatThrownBy(() -> tentativaRepository.saveAndFlush(new Tentativa(simulado, aluno)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_tentativas_simulado_aluno");
    }

    @Test
    void responderDeNovoTrocaAAlternativaSemCriarOutraLinha() {
        var tentativa = tentativaRepository.save(new Tentativa(simulado, aluno)).getId();

        respostaRepository.registrar(tentativa, q1.getId(), alternativa(q1, "B"), false);
        respostaRepository.registrar(tentativa, q1.getId(), alternativa(q1, "A"), true);

        assertThat(respostaRepository.findByTentativaId(tentativa))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.getAlternativaId()).isEqualTo(alternativa(q1, "A"));
                    assertThat(r.isCorreta()).isTrue();
                    assertThat(r.getRespondidaEm()).isNotNull();
                });
    }

    @Test
    void desmarcarDeixaAQuestaoEmBrancoSemMexerNasOutras() {
        var tentativa = tentativaRepository.save(new Tentativa(simulado, aluno)).getId();
        respostaRepository.registrar(tentativa, q1.getId(), alternativa(q1, "A"), true);
        respostaRepository.registrar(tentativa, q2.getId(), alternativa(q2, "C"), false);

        respostaRepository.apagar(tentativa, q1.getId());

        assertThat(respostaRepository.findByTentativaId(tentativa))
                .extracting(Resposta::getQuestaoId)
                .containsExactly(q2.getId());
    }

    @Test
    void bancoRecusaAlternativaDeOutraQuestao() {
        var tentativa = tentativaRepository.save(new Tentativa(simulado, aluno)).getId();

        // Alternativa B da q2 marcada como resposta da q1: a FK composta da V11 barra
        assertThatThrownBy(() -> respostaRepository.registrar(tentativa, q1.getId(), alternativa(q2, "B"), false))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_respostas_alternativa_da_questao");
    }

    // --- apoio ---

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private UUID usuario(String email) {
        var usuario = new Usuario();
        usuario.setNome("Aluna");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        return usuarioRepository.save(usuario).getId();
    }

    private static Long alternativa(Questao questao, String letra) {
        return questao.getAlternativas().stream()
                .filter(a -> a.getLetra().equals(letra))
                .findFirst().orElseThrow()
                .getId();
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
