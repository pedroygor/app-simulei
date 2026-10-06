package br.com.simulados.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

/**
 * SEM @EnableSpringDataWebSupport, de propósito. A anotação cria a configuração
 * web do Spring Data na mão, e aí a autoconfiguração do Boot recua — levando
 * junto TODAS as propriedades spring.data.web.pageable.* do application.yml. Foi
 * assim que o teto de 100 por página nunca valeu (achado em 06/10/2026: ?size=1000
 * devolvia 1000). O formato do JSON (VIA_DTO) agora vem da propriedade
 * `serialization-mode`, junto com o teto.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path diretorioDeImagens;
    private final String baseUrlDeImagens;

    WebConfig(@Value("${storage.imagens.diretorio}") String diretorio,
              @Value("${storage.imagens.base-url}") String baseUrl) {
        // Absoluto: o ResourceHandler resolve "file:" contra o diretório de
        // trabalho do processo, que muda dependendo de como a app foi iniciada
        this.diretorioDeImagens = Path.of(diretorio).toAbsolutePath().normalize();
        this.baseUrlDeImagens = baseUrl;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CabecalhosDeImagemInterceptor())
                .addPathPatterns(baseUrlDeImagens + "/**");
    }

    /**
     * Serve as imagens que o job de importação baixou. Sem isto, as URLs que o
     * importador gravou no markdown das questões apontariam pra lugar nenhum.
     *
     * O ResourceHandler do Spring resolve o caminho contra o diretório base e
     * recusa o que escapa dele — a proteção contra path traversal não depende
     * só da higienização feita na hora de gravar.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(baseUrlDeImagens + "/**")
                .addResourceLocations(diretorioDeImagens.toUri().toString())
                // Nome de arquivo é UUID vindo da fonte: o conteúdo nunca muda,
                // então o navegador pode guardar à vontade
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
