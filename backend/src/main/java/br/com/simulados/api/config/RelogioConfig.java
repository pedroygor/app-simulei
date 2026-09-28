package br.com.simulados.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelogioConfig {

    /**
     * Relógio como bean pra quem depende de tempo poder ser testado.
     *
     * Sem isto, testar o bloqueio por tentativas exigiria esperar 15 minutos
     * de verdade — ou seja, ninguém testaria. Com o Clock injetado, o teste
     * troca por um relógio fixo e avança o tempo na mão.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
