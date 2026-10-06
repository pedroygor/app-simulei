package br.com.simulados.api.questao.application;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Banco real (Testcontainers): o que interessa é a coluna GERADA `disponivel` da
// V9 — quem a calcula é o Postgres, então só um banco de verdade prova que a
// questão marcada some da busca, do detalhe e da contagem por assunto
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MarcadorDeIncompletasTest {

    @Autowired
    private MarcadorDeIncompletas marcador;

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private AssuntoRepository assuntoRepository;

    @Autowired
    private EntityManager entityManager;

    private Long semTexto;
    private Long boa;

    @BeforeEach
    void setUp() {
        semTexto = questaoRepository.save(questao(2016, 165)).getId();
        boa = questaoRepository.save(questao(2016, 166)).getId();
        sincronizar();
    }

    @Test
    void questaoMarcadaSomeDaBuscaDoDetalheEDaContagem() {
        var contagemAntes = contagemDeProbabilidade();

        var resultado = marcador.marcar(List.of(linha(2, 2016, 165)));
        sincronizar();

        assertThat(resultado.marcadas()).isEqualTo(1);
        assertThat(questaoRepository.buscarComAlternativas(semTexto)).isEmpty();
        assertThat(questaoRepository.buscarComAlternativas(boa)).isPresent();
        var busca = questaoRepository.buscar(Area.MATEMATICA, null, 2016, 2016, null, null, PageRequest.of(0, 10));
        assertThat(busca.getContent()).extracting(r -> r.numero()).containsExactly(166);
        assertThat(contagemDeProbabilidade()).isEqualTo(contagemAntes - 1);
    }

    @Test
    void sairDoArquivoLiberaAQuestao() {
        marcador.marcar(List.of(linha(2, 2016, 165)));
        sincronizar();

        // O arquivo é a lista completa: sem a linha, a marcação se desfaz
        var resultado = marcador.marcar(List.of());
        sincronizar();

        assertThat(resultado.liberadas()).isEqualTo(1);
        assertThat(questaoRepository.buscarComAlternativas(semTexto)).isPresent();
    }

    @Test
    void segundaRodadaComOMesmoArquivoNaoAlteraNada() {
        var linhas = List.of(linha(2, 2016, 165));
        marcador.marcar(linhas);
        sincronizar();

        var segunda = marcador.marcar(linhas);

        assertThat(segunda.marcadas()).isZero();
        assertThat(segunda.liberadas()).isZero();
    }

    @Test
    void recusaQuestaoRepetidaSemGravarNada() {
        assertThatThrownBy(() -> marcador.marcar(List.of(linha(2, 2016, 165), linha(3, 2016, 165))))
                .isInstanceOf(IncompletasInvalidasException.class)
                .hasMessageContaining("mais de uma vez");
        sincronizar();

        assertThat(questaoRepository.buscarComAlternativas(semTexto)).isPresent();
    }

    @Test
    void linhaSemQuestaoNoBancoEContada() {
        assertThat(marcador.marcar(List.of(linha(2, 2009, 1))).semQuestao()).containsExactly("2009/1");
    }

    // --- apoio ---

    private long contagemDeProbabilidade() {
        return assuntoRepository.listarComContagem(true, List.of(0)).stream()
                .filter(l -> l.assuntoSlug().equals("probabilidade")).findFirst().orElseThrow().questoes();
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private static LinhaDeIncompleta linha(int linha, int ano, int numero) {
        return new LinhaDeIncompleta(linha, ano, numero, (Idioma) null);
    }

    private Questao questao(int ano, int numero) {
        var questao = new Questao();
        questao.setAno(ano);
        questao.setNumero(numero);
        questao.setArea(Area.MATEMATICA);
        questao.setAssunto(assuntoRepository.findAll().stream()
                .filter(a -> a.getSlug().equals("probabilidade")).findFirst().orElseThrow());
        // CHECK da V4: questão com assunto precisa dizer de onde veio a classificação
        questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
        questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
        questao.setEnunciado("Texto-base");
        questao.setComando("Assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId(ano + "/" + numero);
        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
