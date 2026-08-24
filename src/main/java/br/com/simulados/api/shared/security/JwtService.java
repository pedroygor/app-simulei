package br.com.simulados.api.shared.security;

import br.com.simulados.api.usuario.domain.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

// Emite os tokens. A VALIDAÇÃO do access token quem faz é o resource server
// (JwtDecoder no SecurityConfig); aqui só validamos refresh token.
@Service
public class JwtService {

    static final String CLAIM_TIPO = "tipo";
    static final String TIPO_ACCESS = "access";
    static final String TIPO_REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    // Decoder próprio pro refresh: o decoder do resource server rejeita
    // qualquer token que não seja tipo=access, então não serve aqui
    private final NimbusJwtDecoder refreshDecoder;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        var chave = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.refreshDecoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public String gerarAccessToken(Usuario usuario) {
        return gerar(usuario, TIPO_ACCESS, props.accessTtl().getSeconds());
    }

    public String gerarRefreshToken(Usuario usuario) {
        return gerar(usuario, TIPO_REFRESH, props.refreshTtl().getSeconds());
    }

    // Valida assinatura e expiração; devolve o id do usuário dono do token
    public UUID validarRefreshToken(String token) {
        try {
            var jwt = refreshDecoder.decode(token);
            if (!TIPO_REFRESH.equals(jwt.getClaimAsString(CLAIM_TIPO))) {
                // Impede usar um access token (vida curta, exposto ao JS) como refresh
                throw new TokenInvalidoException();
            }
            return UUID.fromString(jwt.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException();
        }
    }

    public long accessTtlSegundos() {
        return props.accessTtl().getSeconds();
    }

    private String gerar(Usuario usuario, String tipo, long ttlSegundos) {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("simulei")
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(ttlSegundos))
                .claim("role", usuario.getRole().name())
                .claim(CLAIM_TIPO, tipo)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
