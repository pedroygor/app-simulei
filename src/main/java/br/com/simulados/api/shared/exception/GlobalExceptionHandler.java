package br.com.simulados.api.shared.exception;

import br.com.simulados.api.shared.security.TokenInvalidoException;
import br.com.simulados.api.usuario.domain.EmailJaCadastradoException;
import br.com.simulados.api.usuario.domain.UsuarioNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Um lugar só traduz exceção -> HTTP. Controllers e services nunca montam
// resposta de erro na mão.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Lançada pelo @Valid quando o payload viola o Bean Validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse trataValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return ErroResponse.de(HttpStatus.BAD_REQUEST, "Há campos inválidos na requisição",
                request.getRequestURI(), campos);
    }

    // 409: o payload é válido, mas conflita com estado existente
    @ExceptionHandler(EmailJaCadastradoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse trataEmailJaCadastrado(EmailJaCadastradoException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // Cobre login com credencial errada e usuário inativo. Mensagem única de
    // propósito: não revelar se o e-mail existe
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponse trataAutenticacao(AuthenticationException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos", request.getRequestURI());
    }

    @ExceptionHandler(TokenInvalidoException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResponse trataTokenInvalido(TokenInvalidoException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse trataUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // Fallback pra regras de negócio sem tratamento mais específico acima
    @ExceptionHandler(NegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse trataNegocio(NegocioException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request.getRequestURI());
    }
}
