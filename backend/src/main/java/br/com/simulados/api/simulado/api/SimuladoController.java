package br.com.simulados.api.simulado.api;

import br.com.simulados.api.simulado.application.CriacaoDeSimulado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/simulados")
@RequiredArgsConstructor
public class SimuladoController {

    private final CriacaoDeSimulado criacao;

    /**
     * Monta uma prova. ALUNO e PROFESSOR podem (exige só estar autenticado — regra
     * geral do SecurityConfig): o aluno monta o próprio treino, o professor a prova
     * da turma (Etapa 5).
     *
     * O dono é o `sub` do token, nunca um campo do corpo (seção 8.1): quem manda o
     * pedido não escolhe em nome de quem o simulado é criado.
     */
    @PostMapping
    public ResponseEntity<SimuladoCriadoResponse> criar(@Valid @RequestBody CriarSimuladoRequest request,
                                                        @AuthenticationPrincipal Jwt jwt,
                                                        UriComponentsBuilder uriBuilder) {
        var criado = criacao.criar(UUID.fromString(jwt.getSubject()), request.paraPedido());

        // 201 + Location, mesmo contrato do POST /usuarios. O GET /simulados/{id}
        // vem no próximo incremento
        var location = uriBuilder.path("/simulados/{id}").buildAndExpand(criado.simulado().getId()).toUri();
        return ResponseEntity.created(location).body(SimuladoCriadoResponse.de(criado));
    }
}
