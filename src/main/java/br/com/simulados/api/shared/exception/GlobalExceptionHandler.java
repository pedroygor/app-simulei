package br.com.simulados.api.shared.exception;

import br.com.simulados.api.shared.security.TokenInvalidoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Um lugar só traduz exceção -> HTTP. Controllers e services nunca montam
// resposta de erro na mão. Este handler conhece apenas as BASES semânticas
// (RecursoNaoEncontrado -> 404, Conflito -> 409): domínio novo cria exceções
// estendendo as bases e nada muda aqui.
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

    // JSON malformado ou valor impossível de converter (ex: role inexistente).
    // Sem este handler o Spring despacharia pro /error — e viraria 401 (ver
    // dispatcherTypeMatchers no SecurityConfig). Mensagem genérica de propósito:
    // o parse do Jackson vaza detalhe de implementação
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse trataCorpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado",
                request.getRequestURI());
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

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse trataNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConflitoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse trataConflito(ConflitoException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // Backstop pra corrida: a checagem amigável do service pode perder pra uma
    // request concorrente, e aí a constraint do banco estoura aqui. Mensagem
    // genérica: a exceção do driver vaza nome de constraint e SQL
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResponse trataIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.CONFLICT, "A operação conflita com dados já existentes",
                request.getRequestURI());
    }

    // Fallback pra regras de negócio sem base semântica específica
    @ExceptionHandler(NegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse trataNegocio(NegocioException ex, HttpServletRequest request) {
        return ErroResponse.de(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request.getRequestURI());
    }
}
