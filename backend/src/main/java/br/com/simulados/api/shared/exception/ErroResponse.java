package br.com.simulados.api.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

// NON_NULL: "campos" só aparece no JSON quando é erro de validação;
// nos demais erros o campo some em vez de vir como null
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        List<CampoInvalido> campos
) {

    public static ErroResponse de(HttpStatus status, String mensagem, String path) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, null);
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String path, List<CampoInvalido> campos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, path, campos);
    }

    public record CampoInvalido(String campo, String mensagem) {
    }
}
