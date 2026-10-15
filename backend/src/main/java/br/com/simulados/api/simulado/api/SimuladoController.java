package br.com.simulados.api.simulado.api;

import br.com.simulados.api.simulado.application.ConsultaDeSimulado;
import br.com.simulados.api.simulado.application.CriacaoDeSimulado;
import br.com.simulados.api.simulado.application.HistoricoDeProvas;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final ConsultaDeSimulado consulta;
    private final HistoricoDeProvas historico;

    /**
     * Monta uma prova. Exige só estar autenticado (regra geral do SecurityConfig):
     * o aluno monta o próprio treino.
     *
     * O dono é o `sub` do token, nunca um campo do corpo (seção 8.1): quem manda o
     * pedido não escolhe em nome de quem o simulado é criado.
     */
    @PostMapping
    public ResponseEntity<SimuladoCriadoResponse> criar(@Valid @RequestBody CriarSimuladoRequest request,
                                                        @AuthenticationPrincipal Jwt jwt,
                                                        UriComponentsBuilder uriBuilder) {
        var criado = criacao.criar(UUID.fromString(jwt.getSubject()), request.paraPedido());

        // 201 + Location, mesmo contrato do POST /usuarios
        var location = uriBuilder.path("/simulados/{id}").buildAndExpand(criado.simulado().getId()).toUri();
        return ResponseEntity.created(location).body(SimuladoCriadoResponse.de(criado));
    }

    /**
     * "Minhas provas": os simulados do usuário autenticado, do mais novo pro mais
     * velho, com a situação (não iniciada, em andamento, entregue) e a nota das
     * entregues. Paginado (seção 8.4): `?page=&size=`, teto global de 100 por
     * página. Um `?sort=` é ignorado — a ordem é fixa (ver HistoricoDeProvas).
     *
     * @PageableDefault: o tamanho quando o cliente não manda `size`.
     */
    @GetMapping
    public Page<ProvaDoHistoricoResponse> minhas(@AuthenticationPrincipal Jwt jwt,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return historico.listar(UUID.fromString(jwt.getSubject()), pageable).map(ProvaDoHistoricoResponse::de);
    }

    /**
     * O simulado do usuário autenticado. Alheio ou inexistente: 404 igual.
     *
     * O {id} chega como UUID pelo binding do Spring; texto que não é UUID vira 400
     * pelo GlobalExceptionHandler (sem vazar o nome da classe), antes de tocar o banco.
     */
    @GetMapping("/{id}")
    public SimuladoResponse porId(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return SimuladoResponse.de(consulta.detalhar(id, UUID.fromString(jwt.getSubject())));
    }
}
