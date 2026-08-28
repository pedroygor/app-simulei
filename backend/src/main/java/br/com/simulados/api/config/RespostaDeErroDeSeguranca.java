package br.com.simulados.api.config;

import br.com.simulados.api.shared.exception.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
// Spring Security 7: AccessDeniedHandler mora em web.access (o
// AuthenticationEntryPoint continua na raiz de web)
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Faz o Spring Security responder no MESMO formato de erro do resto da API.
 *
 * Sem isto, 401 e 403 saem do filtro antes de chegar em qualquer controller —
 * ou seja, longe do GlobalExceptionHandler — e o cliente recebe corpo vazio.
 * O frontend teria que tratar dois formatos: JSON pros erros de negócio e
 * "adivinhe pelo status" pros de autenticação.
 */
@Component
public class RespostaDeErroDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    RespostaDeErroDeSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Sem token, token expirado ou token inválido. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Mensagem genérica de propósito: dizer "token expirado" vs "assinatura
        // inválida" ajuda mais quem está sondando do que quem está integrando
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Autenticação necessária ou token inválido");
    }

    /** Autenticado, mas sem permissão (ex: ALUNO numa rota de PROFESSOR). */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErroResponse.de(status, mensagem, request.getRequestURI()));
    }
}
