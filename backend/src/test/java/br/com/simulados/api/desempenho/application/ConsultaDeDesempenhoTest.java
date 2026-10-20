package br.com.simulados.api.desempenho.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.desempenho.domain.Aproveitamento;
import br.com.simulados.api.desempenho.domain.AproveitamentoNoAssunto;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.application.ExecucaoDaProva;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Prova entregue com 4 questões, gabarito A:
 *   q1 probabilidade (Matemática): A, certa
 *   q2 probabilidade (Matemática): em branco
 *   q3 ecologia (Natureza):        B, errada
 *   q4 SEM assunto (Matemática):   A, certa
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ConsultaDeDesempenhoTest {

    @Autowired
    private ConsultaDeDesempenho consulta;

    @Autowired
    private ExecucaoDaProva execucao;

    @Autowired
    private SimuladoRepository simuladoRepository;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private AssuntoRepository assuntoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID aluna;

    @BeforeEach
    void setUp() {
        aluna = usuario("aluna@exemplo.com");
        Map<String, Assunto> assuntos = assuntoRepository.findAll().stream()
                .collect(Collectors.toMap(Assunto::getSlug, a -> a));
        var q = new ArrayList<Questao>();
        q.add(questaoRepository.save(questao(1, Area.MATEMATICA, assuntos.get("probabilidade"))));
        q.add(questaoRepository.save(questao(2, Area.MATEMATICA, assuntos.get("probabilidade"))));
        q.add(questaoRepository.save(questao(3, Area.CIENCIAS_NATUREZA, assuntos.get("ecologia"))));
        q.add(questaoRepository.save(questao(4, Area.MATEMATICA, null)));

        var simulado = simuladoRepository.save(
                new Simulado(aluna, "Prova", null, q.stream().map(Questao::getId).toList())).getId();
        sincronizar();
        var tentativa = execucao.iniciar(simulado, aluna).tentativa().getId();
        execucao.responder(tentativa, aluna, q.get(0).getId(), alternativa(q.get(0), "A"));
        execucao.responder(tentativa, aluna, q.get(2).getId(), alternativa(q.get(2), "B"));
        execucao.responder(tentativa, aluna, q.get(3).getId(), alternativa(q.get(3), "A"));
        execucao.finalizar(tentativa, aluna);
        sincronizar();
    }

    @Test
    void geralEAreasSomamTudoInclusiveQuestaoSemAssunto() {
        var desempenho = consulta.doAluno(aluna);

        assertThat(desempenho.provas()).isEqualTo(1);
        // 3 respondidas, 2 certas, 1 em branco: 66% sobre as respondidas
        assertThat(desempenho.geral()).isEqualTo(new Aproveitamento(3, 2, 1));
        assertThat(desempenho.geral().percentual()).isEqualTo(66);
        assertThat(desempenho.porArea()).containsExactly(
                Map.entry(Area.CIENCIAS_NATUREZA, new Aproveitamento(1, 0, 0)),
                Map.entry(Area.MATEMATICA, new Aproveitamento(2, 2, 1)));
    }

    @Test
    void questaoSemAssuntoNaoViraLinhaDeAssunto() {
        assertThat(consulta.doAluno(aluna).porAssunto())
                .extracting(AproveitamentoNoAssunto::assuntoSlug, AproveitamentoNoAssunto::aproveitamento)
                .containsExactlyInAnyOrder(
                        tuple("probabilidade", new Aproveitamento(1, 1, 1)),
                        tuple("ecologia", new Aproveitamento(1, 0, 0)));
    }

    @Test
    void alunoSemProvaTemTudoVazioSemDividirPorZero() {
        var desempenho = consulta.doAluno(usuario("nova@exemplo.com"));

        assertThat(desempenho.provas()).isZero();
        assertThat(desempenho.geral().percentual()).isNull();
        assertThat(desempenho.porArea()).isEmpty();
        assertThat(desempenho.porAssunto()).isEmpty();
    }

    @Test
    void duasQueriesQualquerQueSejaONumeroDeProvas() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();

        consulta.doAluno(aluna);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
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

    private static Questao questao(int numero, Area area, Assunto assunto) {
        var questao = new Questao();
        questao.setAno(2022);
        questao.setNumero(numero + 100);
        questao.setArea(area);
        if (assunto != null) {
            questao.setAssunto(assunto);
            questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
            questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        }
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId("2022/" + (numero + 100));
        for (var letra : List.of("A", "B", "C", "D", "E")) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
