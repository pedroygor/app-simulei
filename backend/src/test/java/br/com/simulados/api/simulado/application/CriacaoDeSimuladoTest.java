package br.com.simulados.api.simulado.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.simulado.application.PedidoDeSimulado.Escolha;
import br.com.simulados.api.simulado.domain.QuestaoDoSimulado;
import br.com.simulados.api.simulado.domain.TipoDeEscolha;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static br.com.simulados.api.simulado.domain.TipoDeEscolha.AREA;
import static br.com.simulados.api.simulado.domain.TipoDeEscolha.ASSUNTO;
import static br.com.simulados.api.simulado.domain.TipoDeEscolha.MATERIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real: o service junta taxonomia (V5), contagem e sorteio nativos. O número
// da questão diz de qual assunto ela é: probabilidade 1xx, estatística 2xx,
// ecologia 3xx, interpretação 4xx, inglês 5xx, espanhol 6xx
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CriacaoDeSimuladoTest {

    @Autowired
    private CriacaoDeSimulado criacao;

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

    private UUID dono;

    @BeforeEach
    void setUp() {
        var usuario = new Usuario();
        usuario.setNome("Aluna");
        usuario.setEmail("aluna@exemplo.com");
        usuario.setSenhaHash("hash-irrelevante-aqui");
        usuario.setRole(Role.ALUNO);
        dono = usuarioRepository.save(usuario).getId();

        var assuntos = assuntoRepository.findAll().stream().collect(Collectors.toMap(Assunto::getSlug, a -> a));
        criar(assuntos.get("probabilidade"), 100, 8, null);
        criar(assuntos.get("estatistica"), 200, 8, null);
        criar(assuntos.get("ecologia"), 300, 8, null);
        criar(assuntos.get("interpretacao-de-texto"), 400, 8, null);
        criar(assuntos.get("interpretacao-ingles"), 500, 3, Idioma.INGLES);
        criar(assuntos.get("interpretacao-espanhol"), 600, 3, Idioma.ESPANHOL);
        sincronizar();
    }

    @Test
    void provaMultidisciplinarDeAreaEMateria() {
        var criado = criacao.criar(dono, pedido(10, List.of(), null,
                escolha(AREA, "CIENCIAS_NATUREZA", null), escolha(MATERIA, "matematica", null)));
        sincronizar();

        assertThat(criado.composicao()).extracting(SimuladoCriado.Parte::rotulo)
                .containsExactly("Ciências da Natureza", "Matemática");
        assertThat(criado.composicao()).extracting(SimuladoCriado.Parte::quantidade).containsExactly(5, 5);
        var numeros = numerosDaProva(criado);
        assertThat(numeros).hasSize(10).doesNotHaveDuplicates();
        assertThat(numeros.subList(0, 5)).allMatch(n -> n > 300 && n < 400);  // Natureza primeiro
        assertThat(numeros.subList(5, 10)).allMatch(n -> n > 100 && n < 300);
    }

    @Test
    void listaDeAssuntosComQuantidadeFixaEDistribuida() {
        var criado = criacao.criar(dono, pedido(10, List.of(), null,
                escolha(ASSUNTO, "probabilidade", 6), escolha(ASSUNTO, "estatistica", null)));

        assertThat(criado.composicao()).extracting(SimuladoCriado.Parte::quantidade).containsExactly(6, 4);
    }

    @Test
    void linguagensSemIdiomaDeixaALinguaEstrangeiraDeFora() {
        // Linguagens tem 8 de português e 3+3 de língua estrangeira: sem idioma, só as 8
        var criado = criacao.criar(dono, pedido(8, List.of(), null, escolha(AREA, "LINGUAGENS", null)));

        assertThat(numerosDaProva(criado)).allMatch(n -> n > 400 && n < 500);
    }

    @Test
    void linguagensComInglesNuncaTrazEspanhol() {
        var criado = criacao.criar(dono, pedido(11, List.of(), Idioma.INGLES, escolha(AREA, "LINGUAGENS", null)));

        var numeros = numerosDaProva(criado);
        assertThat(numeros).hasSize(11).noneMatch(n -> n > 600);
        assertThat(numeros).filteredOn(n -> n > 500).hasSize(3);
    }

    @Test
    void respeitaOsAnos() {
        // As questões ímpares são de 2023, as pares de 2022
        var criado = criacao.criar(dono, pedido(5, List.of(2022), null, escolha(MATERIA, "matematica", null)));

        assertThat(numerosDaProva(criado)).allMatch(n -> n % 2 == 0);
    }

    @Test
    void escolhaDentroDeOutraEhRecusadaNomeandoAsDuas() {
        assertThatThrownBy(() -> criacao.criar(dono, pedido(10, List.of(), null,
                escolha(MATERIA, "matematica", null), escolha(ASSUNTO, "probabilidade", 5))))
                .isInstanceOf(PedidoDeSimuladoInvalidoException.class)
                .hasMessage("Probabilidade já está dentro de Matemática: escolha um dos dois");
    }

    @Test
    void mesmaEscolhaDuasVezesEhRecusada() {
        assertThatThrownBy(() -> criacao.criar(dono, pedido(10, List.of(), null,
                escolha(ASSUNTO, "probabilidade", null), escolha(ASSUNTO, "probabilidade", null))))
                .isInstanceOf(PedidoDeSimuladoInvalidoException.class)
                .hasMessageContaining("aparece mais de uma vez");
    }

    @Test
    void valorInexistenteEhRecusado() {
        assertThatThrownBy(() -> criacao.criar(dono, pedido(5, List.of(), null, escolha(ASSUNTO, "astrologia", null))))
                .isInstanceOf(PedidoDeSimuladoInvalidoException.class)
                .hasMessageContaining("'astrologia' não é um assunto");
        assertThatThrownBy(() -> criacao.criar(dono, pedido(5, List.of(), null, escolha(AREA, "ARTES", null))))
                .isInstanceOf(PedidoDeSimuladoInvalidoException.class)
                .hasMessageContaining("'ARTES' não é uma área");
    }

    @Test
    void anoSemQuestaoDizQualEscolhaFalta() {
        // 2024 ainda não está na base
        assertThatThrownBy(() -> criacao.criar(dono, pedido(5, List.of(2024), null, escolha(ASSUNTO, "probabilidade", null))))
                .isInstanceOf(QuestoesInsuficientesException.class)
                .hasMessageContaining("Probabilidade não tem questões nos anos escolhidos");
        assertThat(simuladoRepository.count()).isZero();
    }

    @Test
    void numeroDeQueriesNaoCresceComONumeroDeEscolhas() {
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();

        statistics.clear();
        criacao.criar(dono, pedido(5, List.of(), null, escolha(ASSUNTO, "probabilidade", null)));
        entityManager.flush();
        var comUma = statistics.getPrepareStatementCount();

        statistics.clear();
        criacao.criar(dono, pedido(9, List.of(), null, escolha(ASSUNTO, "probabilidade", null),
                escolha(ASSUNTO, "estatistica", null), escolha(AREA, "CIENCIAS_NATUREZA", null)));
        entityManager.flush();

        // taxonomia + contagem + sorteio + INSERT do simulado + lote das questões
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(comUma).isEqualTo(5);
    }

    // --- apoio ---

    private List<Integer> numerosDaProva(SimuladoCriado criado) {
        sincronizar();
        var ids = simuladoRepository.findById(criado.simulado().getId()).orElseThrow().getQuestoes().stream()
                .map(QuestaoDoSimulado::getQuestaoId).toList();
        Map<Long, Integer> numeroPorId = questaoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Questao::getId, Questao::getNumero));
        return ids.stream().map(numeroPorId::get).toList();
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private static PedidoDeSimulado pedido(int total, List<Integer> anos, Idioma idioma, Escolha... escolhas) {
        return new PedidoDeSimulado("  Prova de teste  ", total, anos, idioma, List.of(escolhas), null);
    }

    private static Escolha escolha(TipoDeEscolha tipo, String valor, Integer quantidade) {
        return new Escolha(tipo, valor, quantidade);
    }

    // `quantas` questões do assunto, numeradas a partir de base+1; ímpar = 2023, par = 2022
    private void criar(Assunto assunto, int base, int quantas, Idioma idioma) {
        for (int n = 1; n <= quantas; n++) {
            var questao = new Questao();
            questao.setAno(n % 2 == 1 ? 2023 : 2022);
            questao.setNumero(base + n);
            questao.setIdioma(idioma);
            questao.setArea(assunto.getArea());
            questao.setAssunto(assunto);
            questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
            questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
            questao.setEnunciado("Texto-base");
            questao.setComando("Assinale a alternativa correta.");
            questao.setGabarito("A");
            questao.setFonte("enem.dev");
            questao.setFonteId(questao.getAno() + "/" + questao.getNumero());
            for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
                var alternativa = new Alternativa();
                alternativa.setLetra(letra);
                alternativa.setTexto("Alternativa " + letra);
                questao.adicionarAlternativa(alternativa);
            }
            questaoRepository.save(questao);
        }
    }
}
