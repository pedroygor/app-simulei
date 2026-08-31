package br.com.simulados.api.usuario.api;

import br.com.simulados.api.shared.security.JwtProperties;
import br.com.simulados.api.shared.security.TokenInvalidoException;
import br.com.simulados.api.usuario.application.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_REFRESH = "refresh_token";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authService.login(request.email(), request.senha());
        return respostaComCookie(tokens);
    }

    // required=false pra cair na NOSSA exceção (401 padronizado) em vez do
    // erro genérico do Spring quando o cookie não vem
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new TokenInvalidoException();
        }
        var tokens = authService.refresh(refreshToken);
        return respostaComCookie(tokens);
    }

    /**
     * Encerra a sessão no NAVEGADOR expirando o cookie de refresh.
     *
     * Não invalida o token no servidor — ele é stateless, e quem tiver uma
     * cópia consegue usá-la até expirar. Para revogação de verdade seria
     * preciso uma lista de tokens revogados no banco, o que só se paga quando
     * houver requisito real (conta comprometida, sessão remota).
     *
     * Mesmo assim isto importa: o aluno usa o computador compartilhado do
     * cursinho, e "sair" precisa impedir que o próximo da fila continue a
     * sessão. Sem expirar o cookie, um refresh traria a sessão de volta.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        var cookieExpirado = ResponseCookie.from(COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                // maxAge zero manda o navegador apagar o cookie agora
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieExpirado.toString())
                .build();
    }

    private ResponseEntity<TokenResponse> respostaComCookie(AuthService.Tokens tokens) {
        var cookie = ResponseCookie.from(COOKIE_REFRESH, tokens.refreshToken())
                // httpOnly: o JS do frontend nunca lê esse cookie (regra 8.1)
                .httpOnly(true)
                // browsers tratam localhost como contexto seguro, então Secure funciona em dev
                .secure(true)
                // só viaja pro endpoint de refresh, não em toda request
                .path("/auth/refresh")
                .maxAge(jwtProperties.refreshTtl())
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresInSeconds()));
    }
}
