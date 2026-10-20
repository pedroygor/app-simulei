package br.com.simulados.api.simulado.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.domain.ProvaEmAndamento;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import br.com.simulados.api.usuario.infrastructure.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Banco real: o GROUP BY com LEFT JOIN nas respostas e o LIMIT são o que se testa. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ProvasEmAndamentoTest {

    @Autowired
    private ProvasEmAndamento emAndamento;

    @Autowired
    private ExecucaoDaProva execucao;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private UUID aluna;
    private final List<Questao> q = new ArrayList<>();

    @BeforeEach
    void setUp() {
        aluna = usuario("aluna@exemplo.com");
        for (int n = 1; n <= 5; n++) q.add(questaoRepository.save(questao(n)));
        sincronizar();
    }

    @Test
    void asTresMaisRecentesPrimeiro() {
        var maisAntiga = abrir(aluna, "Mais antiga", Duration.ofHours(4));
        var terceira = abrir(aluna, "Terceira", Duration.ofHours(3));
        var segunda = abrir(aluna, "Segunda", Duration.ofHours(2));
        var maisNova = abrir(aluna, "Mais nova", Duration.ofHours(1));

        var provas = emAndamento.doAluno(aluna).provas();

        assertThat(provas).extracting(ProvaEmAndamento::tentativaId).containsExactly(maisNova, segunda, terceira);
        assertThat(provas).extracting(ProvaEmAndamento::tentativaId).doesNotContain(maisAntiga);
    }

    @Test
    void entregueVencidaEDeOutraAlunaFicamDeFora() {
        var aberta = abrir(aluna, "Aberta", Duration.ofHours(1));
        var entregue = abrir(aluna, "Entregue", Duration.ofHours(2));
        execucao.finalizar(entregue, aluna);
        var vencida = abrir(aluna, "Vencida", Duration.ofHours(3));
        // Prazo há 1 min: o job vai entregá-la; "Continuar" levaria a uma prova que acabou
        executar("UPDATE tentativas SET prazo = :prazo WHERE id = :id",
                clock.instant().minus(Duration.ofMinutes(1)), vencida);
        abrir(usuario("outra@exemplo.com"), "Alheia", Duration.ofMinutes(30));

        assertThat(emAndamento.doAluno(aluna).provas()).extracting(ProvaEmAndamento::tentativaId)
                .containsExactly(aberta);
    }

    @Test
    void contaQuestoesERespondidasSoDasDisponiveis() {
        var tentativa = abrir(aluna, "Prova", Duration.ofHours(1));
        execucao.responder(tentativa, aluna, q.get(0).getId(), alternativa(q.get(0)));
        execucao.responder(tentativa, aluna, q.get(1).getId(), alternativa(q.get(1)));
        sincronizar();
        // A 2ª respondida é anulada depois: sai das questões e das respondidas
        executar("UPDATE questoes SET anulada = true, gabarito = NULL WHERE id = :id", null, q.get(1).getId());

        var prova = emAndamento.doAluno(aluna).provas().getFirst();

        assertThat(prova.titulo()).isEqualTo("Prova");
        assertThat(prova.questoes()).isEqualTo(4);
        assertThat(prova.respondidas()).isEqualTo(1);
        assertThat(prova.prazo()).isNull();
    }

    @Test
    void umaQuerySo() {
        for (int i = 1; i <= 4; i++) abrir(aluna, "Prova " + i, Duration.ofHours(i));
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        emAndamento.doAluno(aluna);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    // --- apoio ---

    /** Um simulado novo com as 5 questões, iniciado `atras` antes de agora. */
    private UUID abrir(UUID aluno, String titulo, Duration atras) {
        var simulado = simuladoRepository.save(
                new Simulado(aluno, titulo, null, q.stream().map(Questao::getId).toList())).getId();
        sincronizar();
        var tentativa = execucao.iniciar(simulado, aluno).tentativa().getId();
        sincronizar();
        executar("UPDATE tentativas SET iniciado_em = :quando WHERE id = :id", clock.instant().minus(atras), tentativa);
        return tentativa;
    }

    private void executar(String sql, Instant quando, Object id) {
        var consulta = entityManager.createNativeQuery(sql).setParameter("id", id);
        if (quando != null) consulta.setParameter(sql.contains(":prazo") ? "prazo" : "quando", quando);
        consulta.executeUpdate();
        sincronizar();
    }

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

    private static Long alternativa(Questao questao) {
        return questao.getAlternativas().getFirst().getId();
    }

    private static Questao questao(int numero) {
        var questao = new Questao();
        questao.setAno(2017);
        questao.setNumero(numero + 140);
        questao.setArea(Area.MATEMATICA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2017/" + (numero + 140));
        for (var letra : List.of("A", "B", "C", "D", "E")) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
