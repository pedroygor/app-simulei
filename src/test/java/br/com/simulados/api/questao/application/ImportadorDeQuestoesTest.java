package br.com.simulados.api.questao.application;

import br.com.simulados.api.questao.domain.ChaveNaturalQuestao;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.infrastructure.QuestaoRepository;
import br.com.simulados.api.questao.infrastructure.enemdev.AlternativaEnemDev;
import br.com.simulados.api.questao.infrastructure.enemdev.ConversorQuestaoEnemDev;
import br.com.simulados.api.questao.infrastructure.enemdev.EnemDevClient;
import br.com.simulados.api.questao.infrastructure.enemdev.PaginaDeQuestoesEnemDev;
import br.com.simulados.api.questao.infrastructure.enemdev.QuestaoEnemDev;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Client, storage e repositório são mocks: aqui testamos a ORQUESTRAÇÃO
// (idempotência, deduplicação, isolamento de falhas), não rede nem banco.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportadorDeQuestoesTest {

    @Mock
    private EnemDevClient client;

    @Mock
    private ImagemStorage imagemStorage;

    @Mock
    private QuestaoRepository questaoRepository;

    private ImportadorDeQuestoes importador;

    @BeforeEach
    void setUp() {
        // Conversor real: queremos o comportamento de verdade dele aqui
        importador = new ImportadorDeQuestoes(client, new ConversorQuestaoEnemDev(), imagemStorage, questaoRepository);
        when(questaoRepository.buscarChavesDoAno(anyInt())).thenReturn(List.of());
        when(imagemStorage.guardar(anyString(), anyInt(), anyInt())).thenReturn("/imagens/local/x.png");
    }

    @Test
    void importaQuestoesNovas() {
        responderComPagina("espanhol", questao(2023, 1, "espanhol"), questao(2023, 2, null));
        responderComPagina("ingles", questao(2023, 1, "ingles"));

        var resultado = importador.importarAno(2023);

        // 2 do passe espanhol + 1 variante inglesa (a q2 repetida é ignorada)
        assertThat(resultado.importadas()).isEqualTo(3);
        assertThat(resultado.jaExistiam()).isZero();
        verify(questaoRepository, times(3)).save(any(Questao.class));
    }

    @Test
    void naoReimportaQuestaoQueJaExiste() {
        when(questaoRepository.buscarChavesDoAno(2023))
                .thenReturn(List.of(new ChaveNaturalQuestao(1, Idioma.ESPANHOL)));
        responderComPagina("espanhol", questao(2023, 1, "espanhol"), questao(2023, 2, null));
        responderComPagina("ingles");

        var resultado = importador.importarAno(2023);

        assertThat(resultado.jaExistiam()).isEqualTo(1);
        assertThat(resultado.importadas()).isEqualTo(1);
        verify(questaoRepository, times(1)).save(any(Questao.class));
    }

    @Test
    void consultaChavesUmaVezPorAnoEmVezDeUmaPorQuestao() {
        // Blindagem contra N+1 (seção 8.4): se alguém trocar a consulta em
        // bloco por um exists() dentro do laço, este teste quebra
        var muitas = new QuestaoEnemDev[30];
        for (var i = 0; i < muitas.length; i++) {
            muitas[i] = questao(2023, i + 1, null);
        }
        responderComPagina("espanhol", muitas);
        responderComPagina("ingles");

        importador.importarAno(2023);

        verify(questaoRepository, times(1)).buscarChavesDoAno(2023);
        verify(questaoRepository, never()).existsByAnoAndNumeroAndIdioma(any(), any(), any());
    }

    @Test
    void deduplicaSobreposicaoDaPaginacaoDaFonte() {
        // A enem.dev usa offset inclusivo: a última questão de uma página
        // reaparece como primeira da seguinte
        when(client.buscarQuestoes(eq(2023), anyInt(), eq(0), eq("espanhol")))
                .thenReturn(pagina(true, questao(2023, 1, null), questao(2023, 2, null)));
        when(client.buscarQuestoes(eq(2023), anyInt(), eq(50), eq("espanhol")))
                .thenReturn(pagina(false, questao(2023, 2, null), questao(2023, 3, null)));
        responderComPagina("ingles");

        var resultado = importador.importarAno(2023);

        assertThat(resultado.importadas()).isEqualTo(3);
        verify(questaoRepository, times(3)).save(any(Questao.class));
    }

    @Test
    void contaQuestaoInvalidaSemAbortarOAno() {
        var invalida = new QuestaoEnemDev(5, 2023, "matematica", null, "enunciado", "comando", "A",
                List.of(), alternativas().subList(0, 4));
        responderComPagina("espanhol", questao(2023, 1, null), invalida, questao(2023, 6, null));
        responderComPagina("ingles");

        var resultado = importador.importarAno(2023);

        assertThat(resultado.ignoradasPorDadoInvalido()).isEqualTo(1);
        // As questões seguintes continuam entrando: uma ruim não derruba o ano
        assertThat(resultado.importadas()).isEqualTo(2);
    }

    @Test
    void mantemUrlDaFonteQuandoDownloadDeImagemFalha() {
        when(imagemStorage.guardar(anyString(), anyInt(), anyInt()))
                .thenThrow(new ImagemIndisponivelException("https://enem.dev/x.png", new RuntimeException("404")));
        var comImagem = new QuestaoEnemDev(1, 2023, "matematica", null,
                "Veja ![](https://enem.dev/2023/questions/1/x.png)", "comando", "A",
                List.of("https://enem.dev/2023/questions/1/x.png"), alternativas());
        responderComPagina("espanhol", comImagem);
        responderComPagina("ingles");

        var resultado = importador.importarAno(2023);

        assertThat(resultado.importadas()).isEqualTo(1);
        assertThat(resultado.imagensComFalha()).isEqualTo(1);

        var capturada = ArgumentCaptor.forClass(Questao.class);
        verify(questaoRepository).save(capturada.capture());
        // Fallback: perder a questão inteira por causa de um PNG fora do ar
        // seria pior do que continuar apontando pra fonte
        assertThat(capturada.getValue().getEnunciado()).contains("https://enem.dev/2023/questions/1/x.png");
    }

    @Test
    void somaResultadosDeVariosAnos() {
        responderComPagina("espanhol", questao(2022, 1, null));
        responderComPagina("ingles");
        when(client.buscarQuestoes(eq(2023), anyInt(), anyInt(), eq("espanhol")))
                .thenReturn(pagina(false, questao(2023, 1, null), questao(2023, 2, null)));

        var resultado = importador.importarAnos(List.of(2022, 2023));

        assertThat(resultado.importadas()).isEqualTo(3);
    }

    private void responderComPagina(String idioma, QuestaoEnemDev... questoes) {
        when(client.buscarQuestoes(anyInt(), anyInt(), anyInt(), eq(idioma)))
                .thenReturn(pagina(false, questoes));
    }

    private PaginaDeQuestoesEnemDev pagina(boolean temMais, QuestaoEnemDev... questoes) {
        return new PaginaDeQuestoesEnemDev(
                new PaginaDeQuestoesEnemDev.Metadados(50, 0, questoes.length, temMais),
                List.of(questoes));
    }

    private QuestaoEnemDev questao(int ano, int numero, String idioma) {
        return new QuestaoEnemDev(numero, ano, "matematica", idioma,
                "Texto-base", "Assinale a correta.", "A", List.of(), alternativas());
    }

    private List<AlternativaEnemDev> alternativas() {
        return List.of(
                new AlternativaEnemDev("A", "Alternativa A", null, true),
                new AlternativaEnemDev("B", "Alternativa B", null, false),
                new AlternativaEnemDev("C", "Alternativa C", null, false),
                new AlternativaEnemDev("D", "Alternativa D", null, false),
                new AlternativaEnemDev("E", "Alternativa E", null, false));
    }
}
