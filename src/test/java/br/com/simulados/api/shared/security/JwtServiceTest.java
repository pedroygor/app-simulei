package br.com.simulados.api.shared.security;

import br.com.simulados.api.usuario.domain.Role;
import br.com.simulados.api.usuario.domain.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unitário sem Spring: o JwtService é construído na mão com um segredo de teste
class JwtServiceTest {

    private static final String SEGREDO = "segredo-fake-de-teste-com-mais-de-32-bytes-para-hs256-funcionar";

    private JwtService jwtService;
    private NimbusJwtDecoder decoder;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        var props = new JwtProperties(SEGREDO, Duration.ofMinutes(30), Duration.ofDays(7));
        var chave = new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        jwtService = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(chave)), props);
        decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("ana@teste.com");
        usuario.setRole(Role.ALUNO);
    }

    @Test
    void accessTokenCarregaSubRoleETipo() {
        var token = jwtService.gerarAccessToken(usuario);
        var jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ALUNO");
        assertThat(jwt.getClaimAsString("tipo")).isEqualTo("access");
    }

    @Test
    void validarRefreshTokenDevolveIdDoUsuario() {
        var refresh = jwtService.gerarRefreshToken(usuario);

        assertThat(jwtService.validarRefreshToken(refresh)).isEqualTo(usuario.getId());
    }

    @Test
    void rejeitaAccessTokenUsadoComoRefresh() {
        var access = jwtService.gerarAccessToken(usuario);

        assertThatThrownBy(() -> jwtService.validarRefreshToken(access))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rejeitaRefreshTokenDeOutroIssuer() {
        // Token assinado com o MESMO segredo, mas emitido por "outro sistema":
        // a validação de issuer precisa barrar
        var chave = new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoderExterno = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        var claims = JwtClaimsSet.builder()
                .issuer("outro-sistema")
                .subject(usuario.getId().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("tipo", "refresh")
                .build();
        var token = encoderExterno.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> jwtService.validarRefreshToken(token))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rejeitaTokenAdulterado() {
        var refresh = jwtService.gerarRefreshToken(usuario);
        var adulterado = refresh.substring(0, refresh.length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtService.validarRefreshToken(adulterado))
                .isInstanceOf(TokenInvalidoException.class);
    }
}
