package br.com.simulados.api.usuario.infrastructure.google;

import br.com.simulados.api.usuario.domain.CredencialGoogleInvalidaException;
import br.com.simulados.api.usuario.domain.LoginComGoogleIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As regras do Google conferidas sem rede: o teste faz o papel do Google,
 * assinando tokens com um par de chaves RSA gerado aqui. O verificador recebe a
 * chave pública, como em produção recebe as do Google pela URL de JWKS.
 */
class VerificadorDeTokenDoGoogleNimbusTest {

    private static final String CLIENT_ID = "simulei-teste.apps.googleusercontent.com";
    private static final KeyPair CHAVES_DO_GOOGLE = gerarChaves();
    private static final KeyPair CHAVES_DE_UM_FALSARIO = gerarChaves();

    private final VerificadorDeTokenDoGoogleNimbus verificador = new VerificadorDeTokenDoGoogleNimbus(
            NimbusJwtDecoder.withPublicKey((RSAPublicKey) CHAVES_DO_GOOGLE.getPublic()).build(), CLIENT_ID);

    @Test
    void tokenValidoDevolveAIdentidade() {
        var identidade = verificador.verificar(token(c -> { }));

        assertThat(identidade.sub()).isEqualTo("1234567890");
        assertThat(identidade.email()).isEqualTo("ana.lima@gmail.com");
        assertThat(identidade.emailVerificado()).isTrue();
        assertThat(identidade.nome()).isEqualTo("Ana Lima");
    }

    @Test
    void aceitaOsDoisEmissoresDoGoogle() {
        var semHttps = verificador.verificar(token(c -> c.issuer("accounts.google.com")));

        assertThat(semHttps.sub()).isEqualTo("1234567890");
    }

    @Test
    void recusaTokenQueOGoogleEmitiuParaOutroSite() {
        var deOutroSite = token(c -> c.audience(List.of("outro-site.apps.googleusercontent.com")));

        assertThatThrownBy(() -> verificador.verificar(deOutroSite))
                .isInstanceOf(CredencialGoogleInvalidaException.class);
    }

    @Test
    void recusaOutroEmissor() {
        var deOutroEmissor = token(c -> c.issuer("https://login.exemplo.com"));

        assertThatThrownBy(() -> verificador.verificar(deOutroEmissor))
                .isInstanceOf(CredencialGoogleInvalidaException.class);
    }

    @Test
    void recusaTokenVencido() {
        // Além dos 60 s de tolerância do relógio
        var agora = Instant.now();
        var vencido = token(c -> c.issuedAt(agora.minus(Duration.ofHours(2)))
                .expiresAt(agora.minus(Duration.ofMinutes(5))));

        assertThatThrownBy(() -> verificador.verificar(vencido))
                .isInstanceOf(CredencialGoogleInvalidaException.class);
    }

    @Test
    void recusaTokenAssinadoPorQuemNaoEhOGoogle() {
        // Conteúdo perfeito, chave errada: é o token forjado
        var forjado = token(CHAVES_DE_UM_FALSARIO, c -> { });

        assertThatThrownBy(() -> verificador.verificar(forjado))
                .isInstanceOf(CredencialGoogleInvalidaException.class);
    }

    @Test
    void recusaTextoQueNaoEhToken() {
        assertThatThrownBy(() -> verificador.verificar("isto-nao-e-um-jwt"))
                .isInstanceOf(CredencialGoogleInvalidaException.class);
    }

    @Test
    void recusaTokenSemEmail() {
        var semEmail = token(c -> c.claims(claims -> claims.remove("email")));

        assertThatThrownBy(() -> verificador.verificar(semEmail))
                .isInstanceOf(CredencialGoogleInvalidaException.class);
    }

    @Test
    void emailVerificadoAusenteContaComoNaoVerificado() {
        // Na dúvida, não liga conta pelo e-mail
        var semConfirmacao = token(c -> c.claims(claims -> claims.remove("email_verified")));

        assertThat(verificador.verificar(semConfirmacao).emailVerificado()).isFalse();
    }

    @Test
    void emailVerificadoComoTextoTambemVale() {
        var comTexto = token(c -> c.claim("email_verified", "true"));

        assertThat(verificador.verificar(comTexto).emailVerificado()).isTrue();
    }

    @Test
    void googleInacessivelEhIndisponivelENaoCredencialInvalida() {
        // Porta 9 em localhost: conexão recusada na hora, sem esperar timeout
        var semGoogle = new VerificadorDeTokenDoGoogleNimbus(
                NimbusJwtDecoder.withJwkSetUri("http://127.0.0.1:9/certs").build(), CLIENT_ID);

        assertThatThrownBy(() -> semGoogle.verificar(token(c -> { })))
                .isInstanceOf(LoginComGoogleIndisponivelException.class);
    }

    @Test
    void semClientIdOLoginComGoogleFicaDesligado() {
        var desligado = new GoogleConfig().verificadorDeTokenDoGoogle("", "https://www.googleapis.com/oauth2/v3/certs");

        assertThatThrownBy(() -> desligado.verificar(token(c -> { })))
                .isInstanceOf(LoginComGoogleIndisponivelException.class);
    }

    // Um ID token como o Google emite; cada teste mexe só no que quer provar
    private static String token(Consumer<JwtClaimsSet.Builder> ajuste) {
        return token(CHAVES_DO_GOOGLE, ajuste);
    }

    private static String token(KeyPair chaves, Consumer<JwtClaimsSet.Builder> ajuste) {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("https://accounts.google.com")
                .audience(List.of(CLIENT_ID))
                .subject("1234567890")
                .issuedAt(agora)
                .expiresAt(agora.plus(Duration.ofHours(1)))
                .claim("email", "ana.lima@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Ana Lima");
        ajuste.accept(claims);

        var encoder = NimbusJwtEncoder
                .withKeyPair((RSAPublicKey) chaves.getPublic(), (RSAPrivateKey) chaves.getPrivate())
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private static KeyPair gerarChaves() {
        try {
            var gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(2048);
            return gerador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
