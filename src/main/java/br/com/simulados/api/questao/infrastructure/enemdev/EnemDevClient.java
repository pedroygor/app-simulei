package br.com.simulados.api.questao.infrastructure.enemdev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Component
public class EnemDevClient {

    private static final Logger log = LoggerFactory.getLogger(EnemDevClient.class);

    // A API responde x-ratelimit-limit: 10 / x-ratelimit-reset: 10000, ou seja
    // 10 requisições a cada 10s. Espaçamos 1,1s pra ficar abaixo do teto sem
    // depender de ler o header a cada resposta.
    private static final Duration INTERVALO_ENTRE_CHAMADAS = Duration.ofMillis(1100);
    private static final int MAX_TENTATIVAS = 4;

    private final RestClient restClient;
    // Sem sincronização de propósito: o importador é single-thread. Se algum dia
    // rodar em paralelo, isto precisa virar um rate limiter de verdade.
    private long ultimaChamadaEm;

    public EnemDevClient(RestClient enemDevRestClient) {
        this.restClient = enemDevRestClient;
    }

    public List<ProvaEnemDev> listarProvas() {
        // ParameterizedTypeReference preserva o tipo genérico List<ProvaEnemDev>
        // em runtime — sem ele o Jackson não sabe desserializar a lista
        return executar(() -> restClient.get()
                .uri("/exams")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }));
    }

    // idioma em minúsculo como a API espera ("ingles"/"espanhol"); null omite o
    // parâmetro. Necessário porque as questões 1-5 de linguagens só vêm na
    // variante pedida — o job varre cada ano nos dois idiomas
    public PaginaDeQuestoesEnemDev buscarQuestoes(int ano, int limite, int offset, String idioma) {
        return executar(() -> restClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path("/exams/{ano}/questions")
                            .queryParam("limit", limite)
                            .queryParam("offset", offset);
                    if (idioma != null) {
                        uri.queryParam("language", idioma);
                    }
                    return uri.build(ano);
                })
                .retrieve()
                .body(PaginaDeQuestoesEnemDev.class));
    }

    // Toda falha do RestClient (4xx/5xx, timeout, rede) vira a nossa exceção:
    // quem usa o client não conhece os tipos de erro do Spring.
    // 429 é exceção à regra: em vez de estourar, espera e tenta de novo — a
    // importação inteira são ~120 chamadas e não pode morrer por throttling.
    private <T> T executar(Supplier<T> chamada) {
        for (var tentativa = 1; ; tentativa++) {
            respeitarIntervalo();
            try {
                return chamada.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (tentativa >= MAX_TENTATIVAS) {
                    throw new EnemDevClientException(e);
                }
                // Backoff exponencial: 2s, 4s, 8s
                var espera = Duration.ofSeconds(1L << tentativa);
                log.warn("429 da enem.dev (tentativa {}/{}), esperando {}s",
                        tentativa, MAX_TENTATIVAS, espera.toSeconds());
                dormir(espera);
            } catch (RestClientException e) {
                throw new EnemDevClientException(e);
            }
        }
    }

    private void respeitarIntervalo() {
        var desdeAUltima = System.currentTimeMillis() - ultimaChamadaEm;
        var falta = INTERVALO_ENTRE_CHAMADAS.toMillis() - desdeAUltima;
        if (falta > 0) {
            dormir(Duration.ofMillis(falta));
        }
        ultimaChamadaEm = System.currentTimeMillis();
    }

    private void dormir(Duration duracao) {
        try {
            Thread.sleep(duracao);
        } catch (InterruptedException e) {
            // Preserva o sinal de interrupção pra quem chamou poder abortar
            Thread.currentThread().interrupt();
            throw new EnemDevClientException(e);
        }
    }
}
