package br.com.simulados.api.questao.infrastructure.enemdev;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.function.Supplier;

@Component
public class EnemDevClient {

    private final RestClient restClient;

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
    // quem usa o client não conhece os tipos de erro do Spring
    private <T> T executar(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientException e) {
            throw new EnemDevClientException(e);
        }
    }
}
