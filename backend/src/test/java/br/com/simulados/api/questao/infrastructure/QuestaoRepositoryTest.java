package br.com.simulados.api.questao.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.ConfiancaDaClassificacao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.OrigemDaClassificacao;
import br.com.simulados.api.questao.domain.Questao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Valida que entidade e migration V2 contam a mesma história: o contexto só
// sobe se o ddl-auto=validate aceitar, e os saves exercitam as constraints.
// @Transactional: cada teste roda numa transação revertida no final — o banco
// do container fica limpo entre testes
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class QuestaoRepositoryTest {

    @Autowired
    private QuestaoRepository questaoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void salvaECarregaQuestaoComAlternativas() {
        var questao = questaoValida(2023, 1, Idioma.ESPANHOL);
        questaoRepository.save(questao);

        // flush força o INSERT agora; clear esvazia o cache da sessão — o
        // findById seguinte vai DE VERDADE ao banco, provando o roundtrip
        entityManager.flush();
        entityManager.clear();

        var carregada = questaoRepository.findById(questao.getId()).orElseThrow();

        assertThat(carregada.getAno()).isEqualTo(2023);
        assertThat(carregada.getIdioma()).isEqualTo(Idioma.ESPANHOL);
        assertThat(carregada.getArea()).isEqualTo(Area.LINGUAGENS);
        assertThat(carregada.getGabarito()).isEqualTo("A");
        assertThat(carregada.getAlternativas()).hasSize(5);
        // @OrderBy("letra") garante a ordem vinda do banco
        assertThat(carregada.getAlternativas().getFirst().getLetra()).isEqualTo("A");
        assertThat(carregada.getAlternativas().getLast().getLetra()).isEqualTo("E");
    }

    @Test
    void impedeDuplicataMesmoComIdiomaNulo() {
        // Sem NULLS NOT DISTINCT na constraint, este teste falharia: o padrão
        // SQL trata NULL != NULL e deixaria a segunda passar
        questaoRepository.saveAndFlush(questaoValida(2023, 50, null));

        assertThatThrownBy(() -> questaoRepository.saveAndFlush(questaoValida(2023, 50, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void impedeQuestaoNaoAnuladaSemGabarito() {
        var questao = questaoValida(2023, 60, null);
        questao.setGabarito(null);

        assertThatThrownBy(() -> questaoRepository.saveAndFlush(questao))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aceitaQuestaoAnuladaSemGabarito() {
        var questao = questaoValida(2023, 70, null);
        questao.setGabarito(null);
        questao.setAnulada(true);

        questaoRepository.saveAndFlush(questao);

        assertThat(questao.getId()).isNotNull();
    }

    // --- Taxonomia (V4 + V5) ---

    @Test
    void taxonomiaSemeadaPorInteiro() {
        // A V5 já se autoconfere, mas este teste pega o caso de alguém editar
        // a V5 e a autoconferência junto, errando as duas do mesmo jeito
        var materias = entityManager.createQuery("select count(m) from Materia m", Long.class).getSingleResult();
        var assuntos = entityManager.createQuery("select count(a) from Assunto a", Long.class).getSingleResult();

        assertThat(materias).isEqualTo(14);
        assertThat(assuntos).isEqualTo(118);
    }

    @Test
    void assuntoHerdaAAreaDaMateria() {
        var probabilidade = assunto("probabilidade");

        assertThat(probabilidade.getArea()).isEqualTo(Area.MATEMATICA);
        assertThat(probabilidade.getMateria().getSlug()).isEqualTo("matematica");
    }

    @Test
    void aceitaAssuntoDaMesmaAreaDaQuestao() {
        var questao = questaoValida(2023, 80, null);
        classificar(questao, assunto("interpretacao-de-texto"));

        questaoRepository.saveAndFlush(questao);

        assertThat(questao.getId()).isNotNull();
    }

    @Test
    void recusaAssuntoDeOutraArea() {
        // A questão é de LINGUAGENS; genética é de CIÊNCIAS DA NATUREZA. Quem
        // recusa é a FK composta (assunto_id, area) da V4 — o banco, não o
        // Java. Nem um bug no importador consegue gravar isso
        var questao = questaoValida(2023, 81, null);
        classificar(questao, assunto("genetica"));

        assertThatThrownBy(() -> questaoRepository.saveAndFlush(questao))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void recusaAssuntoSemOrigemDaClassificacao() {
        // Assunto sem dizer quem classificou é dado órfão: sem a origem, a
        // carga da IA não saberia se pode sobrescrever
        var questao = questaoValida(2023, 82, null);
        questao.setAssunto(assunto("interpretacao-de-texto"));

        assertThatThrownBy(() -> questaoRepository.saveAndFlush(questao))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Assunto assunto(String slug) {
        return entityManager.createQuery("select a from Assunto a where a.slug = :slug", Assunto.class)
                .setParameter("slug", slug)
                .getSingleResult();
    }

    private void classificar(Questao questao, Assunto assunto) {
        questao.setAssunto(assunto);
        questao.setClassificacaoOrigem(OrigemDaClassificacao.IA);
        questao.setClassificacaoConfianca(ConfiancaDaClassificacao.ALTA);
    }

    private Questao questaoValida(int ano, int numero, Idioma idioma) {
        var questao = new Questao();
        questao.setAno(ano);
        questao.setNumero(numero);
        questao.setIdioma(idioma);
        questao.setArea(Area.LINGUAGENS);
        questao.setEnunciado("Texto-base da questão em markdown");
        questao.setComando("Com base no texto, assinale a alternativa correta.");
        questao.setGabarito("A");
        questao.setFonte("enem.dev");
        questao.setFonteId(ano + "/" + numero + (idioma != null ? "/" + idioma : ""));

        for (var letra : new String[]{"A", "B", "C", "D", "E"}) {
            var alternativa = new Alternativa();
            alternativa.setLetra(letra);
            alternativa.setTexto("Alternativa " + letra);
            questao.adicionarAlternativa(alternativa);
        }
        return questao;
    }
}
