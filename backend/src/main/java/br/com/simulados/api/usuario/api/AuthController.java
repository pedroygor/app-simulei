package br.com.simulados.api.usuario.api;

import br.com.simulados.api.shared.security.JwtProperties;
import br.com.simulados.api.shared.security.TokenInvalidoException;
import br.com.simulados.api.usuario.application.AuthService;
import br.com.simulados.api.usuario.application.LoginComGoogle;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    private final LoginComGoogle loginComGoogle;
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
     * Entra com o ID token do botão do Google. 404 quando não há conta ligada:
     * é o sinal pro front abrir "completar cadastro" (não é erro da pessoa).
     */
    @PostMapping("/google")
    public ResponseEntity<TokenResponse> entrarComGoogle(@Valid @RequestBody EntrarComGoogleRequest request) {
        var tokens = loginComGoogle.entrar(request.credencial());
        return respostaComCookie(tokens);
    }

    // 201: a conta nasce aqui. Já devolve a sessão, pra não pedir um login a mais
    @PostMapping("/google/cadastro")
    public ResponseEntity<TokenResponse> cadastrarComGoogle(@Valid @RequestBody CadastrarComGoogleRequest request) {
        var tokens = loginComGoogle.cadastrar(request.credencial(), request.nome());
        return respostaComCookie(HttpStatus.CREATED, tokens);
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
        return respostaComCookie(HttpStatus.OK, tokens);
    }

    private ResponseEntity<TokenResponse> respostaComCookie(HttpStatus status, AuthService.Tokens tokens) {
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
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiresInSeconds()));
    }
}
