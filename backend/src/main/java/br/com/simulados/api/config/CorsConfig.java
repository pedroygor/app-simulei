package br.com.simulados.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
public class CorsConfig {

    private final List<String> origensPermitidas;

    CorsConfig(@Value("${app.cors.origens}") List<String> origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    /**
     * O SecurityConfig liga esta fonte no filtro de CORS.
     *
     * Duas coisas andam juntas aqui e não podem ser separadas: como usamos
     * `allowCredentials(true)` (o cookie de refresh precisa viajar), o navegador
     * PROÍBE `Access-Control-Allow-Origin: *`. Por isso a origem é sempre
     * explícita — o que também é a regra 8.1 do projeto, não só exigência do
     * protocolo.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Sem isto o navegador não manda nem aceita o cookie httpOnly do refresh
        config.setAllowCredentials(true);
        // O 201 do cadastro devolve Location; por padrão o JS não enxerga
        // header nenhum além dos "simples"
        config.setExposedHeaders(List.of("Location"));
        // Cacheia o preflight: sem isso todo POST/PUT dispara um OPTIONS antes
        config.setMaxAge(Duration.ofHours(1));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
