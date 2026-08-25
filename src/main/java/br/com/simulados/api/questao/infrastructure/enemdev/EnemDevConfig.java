package br.com.simulados.api.questao.infrastructure.enemdev;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
class EnemDevConfig {

    // O Boot fornece o RestClient.Builder já com os conversores JSON do
    // contexto; aqui só amarramos base URL e timeouts
    @Bean
    RestClient enemDevRestClient(RestClient.Builder builder,
                                 @Value("${enem-dev.base-url}") String baseUrl) {
        // Timeouts explícitos: o default do JDK é esperar indefinidamente,
        // e o job de importação não pode ficar pendurado num socket morto
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
