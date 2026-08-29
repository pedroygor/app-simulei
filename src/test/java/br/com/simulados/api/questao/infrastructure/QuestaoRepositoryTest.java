package br.com.simulados.api.questao.infrastructure;

import br.com.simulados.api.TestcontainersConfiguration;
import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Idioma;
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
