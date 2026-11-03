package br.com.simulados.api.usuario.infrastructure.google;

import br.com.simulados.api.usuario.application.IdentidadeGoogle;
import br.com.simulados.api.usuario.application.VerificadorDeTokenDoGoogle;
import br.com.simulados.api.usuario.domain.CredencialGoogleInvalidaException;
import br.com.simulados.api.usuario.domain.LoginComGoogleIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Collection;
import java.util.Set;

/**
 * Confere o ID token do Google com o mesmo NimbusJwtDecoder que valida os nossos
 * tokens, mas com chave pública RSA (a do Google) em vez do nosso segredo HMAC.
 */
@Slf4j
class VerificadorDeTokenDoGoogleNimbus implements VerificadorDeTokenDoGoogle {

    // O Google emite com os dois valores, conforme a biblioteca que gerou o token
    static final Set<String> EMISSORES = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decoder;

    // Recebe o decoder pronto (de onde vêm as chaves é problema do GoogleConfig):
    // em produção, a URL do Google; no teste, uma chave gerada no próprio teste
    VerificadorDeTokenDoGoogleNimbus(NimbusJwtDecoder decoder, String clientId) {
        this.decoder = decoder;
        this.decoder.setJwtValidator(regras(clientId));
    }

    /**
     * Assinatura o decoder confere sozinho. O resto são as regras do Google:
     * https://developers.google.com/identity/gsi/web/guides/verify-google-id-token
     */
    static OAuth2TokenValidator<Jwt> regras(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                // exp e nbf, com 60 s de tolerância pra relógios desencontrados
                new JwtTimestampValidator(),
                new JwtClaimValidator<String>(JwtClaimNames.ISS,
                        iss -> iss != null && EMISSORES.contains(iss)),
                // A checagem que mais se esquece: sem ela, um token que o Google
                // emitiu pra OUTRO site com login do Google entraria aqui
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(clientId)));
    }

    @Override
    public IdentidadeGoogle verificar(String credencial) {
        Jwt jwt;
        try {
            jwt = decoder.decode(credencial);
        } catch (BadJwtException e) {
            // Token ruim: assinatura, formato, emissor, audiência ou validade
            log.debug("ID token do Google recusado: {}", e.getMessage());
            throw new CredencialGoogleInvalidaException();
        } catch (JwtException e) {
            // Qualquer outra falha do decoder é não ter conseguido buscar as
            // chaves do Google. Não é culpa da pessoa: 503, não 401
            log.warn("Não foi possível buscar as chaves do Google", e);
            throw new LoginComGoogleIndisponivelException(e);
        }

        var email = jwt.getClaimAsString("email");
        if (jwt.getSubject() == null || jwt.getSubject().isBlank() || email == null || email.isBlank()) {
            // O botão do Google sempre manda os dois; sem eles não há quem ligar
            throw new CredencialGoogleInvalidaException();
        }
        return new IdentidadeGoogle(jwt.getSubject(), email, emailVerificado(jwt), jwt.getClaimAsString("name"));
    }

    // Vem como booleano, mas já veio como texto "true" em tokens antigos.
    // Ausente = não verificado: na dúvida, não liga conta pelo e-mail
    private static boolean emailVerificado(Jwt jwt) {
        var valor = jwt.getClaims().get("email_verified");
        return Boolean.TRUE.equals(valor) || "true".equals(valor);
    }
}
