package br.com.simulados.api.config;

import br.com.simulados.api.shared.security.JwtProperties;
import br.com.simulados.api.shared.security.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
// Habilita @PreAuthorize nos métodos (autorização por role fica no endpoint)
@EnableMethodSecurity
// Registra o record JwtProperties como bean preenchido pelo application.yml
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expõe o AuthenticationManager que o Boot monta a partir do nosso
    // UsuarioDetailsService + PasswordEncoder, pro AuthService injetar
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protege sessão em cookie; nossa API é stateless com token
                // no header, onde esse ataque não se aplica
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // O Security também filtra o dispatch de ERRO (o forward interno
                        // pro /error). Sem liberar, qualquer erro que escape do
                        // GlobalExceptionHandler vira 401 em vez de 400/500
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
                        .anyRequest().authenticated())
                // Liga o filtro do resource server: extrai o Bearer do header,
                // valida com o jwtDecoder e povoa o SecurityContext — sem filtro nosso
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(props.chaveHmac()));
    }

    // Decoder usado pelo filtro acima em TODA request autenticada
    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        var decoder = NimbusJwtDecoder.withSecretKey(props.chaveHmac())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Além de assinatura/expiração (default), exige tipo=access:
        // refresh token roubado do cookie não serve pra chamar a API
        OAuth2TokenValidator<Jwt> tipoAccess = jwt ->
                "access".equals(jwt.getClaimAsString("tipo"))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Token não é de acesso", null));
        // createDefaultWithIssuer: só aceita token emitido por nós — um JWT de
        // outro sistema que por azar compartilhe o segredo não passa
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(JwtService.ISSUER), tipoAccess));
        return decoder;
    }

    // Traduz a claim "role" do token em authority ROLE_X, pro hasRole funcionar
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

}
