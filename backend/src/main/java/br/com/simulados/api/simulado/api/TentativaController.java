package br.com.simulados.api.simulado.api;

import br.com.simulados.api.questao.api.QuestaoMapper;
import br.com.simulados.api.simulado.application.ConsultaDeResultado;
import br.com.simulados.api.simulado.application.ConsultaDeTentativa;
import br.com.simulados.api.simulado.application.ExecucaoDaProva;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * Fazer a prova. Exige só estar autenticado (regra geral do SecurityConfig); o
 * aluno é sempre o `sub` do token, nunca um campo da requisição (seção 8.1).
 * Tentativa alheia ou inexistente: 404 igual. {id} que não é UUID: 400.
 */
@RestController
@RequiredArgsConstructor
public class TentativaController {

    private final ExecucaoDaProva execucao;
    private final ConsultaDeTentativa consulta;
    private final ConsultaDeResultado resultados;
    private final QuestaoMapper questaoMapper;

    /**
     * 201 + Location quando cria; 200 com a MESMA tentativa quando já existia
     * (continuar a prova). O cliente trata os dois igual: abre a tentativa do corpo.
     */
    @PostMapping("/simulados/{simuladoId}/tentativas")
    public ResponseEntity<TentativaResponse> iniciar(@PathVariable UUID simuladoId,
                                                     @AuthenticationPrincipal Jwt jwt,
                                                     UriComponentsBuilder uriBuilder) {
        var iniciada = execucao.iniciar(simuladoId, aluno(jwt));
        var corpo = TentativaResponse.de(iniciada.tentativa());
        if (!iniciada.nova()) {
            return ResponseEntity.ok(corpo);
        }
        var location = uriBuilder.path("/tentativas/{id}").buildAndExpand(corpo.id()).toUri();
        return ResponseEntity.created(location).body(corpo);
    }

    @GetMapping("/tentativas/{id}")
    public ProvaResponse prova(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ProvaResponse.de(consulta.prova(id, aluno(jwt)), questaoMapper);
    }

    /**
     * PUT: responder de novo a mesma questão é trocar a resposta, e repetir a mesma
     * requisição dá o mesmo resultado — a semântica do PUT. 204: o cliente já sabe
     * o que marcou, e devolver `correta` aqui entregaria o gabarito.
     */
    @PutMapping("/tentativas/{id}/respostas/{questaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void responder(@PathVariable UUID id, @PathVariable Long questaoId,
                          @Valid @RequestBody ResponderRequest request,
                          @AuthenticationPrincipal Jwt jwt) {
        execucao.responder(id, aluno(jwt), questaoId, request.alternativaId());
    }

    @DeleteMapping("/tentativas/{id}/respostas/{questaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmarcar(@PathVariable UUID id, @PathVariable Long questaoId, @AuthenticationPrincipal Jwt jwt) {
        execucao.desmarcar(id, aluno(jwt), questaoId);
    }

    // POST e não PUT/PATCH no status: finalizar é uma AÇÃO com regra (só uma vez),
    // não a edição livre de um campo
    @PostMapping("/tentativas/{id}/finalizar")
    public TentativaResponse finalizar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return TentativaResponse.de(execucao.finalizar(id, aluno(jwt)));
    }

    /**
     * Nota, placar por área e por assunto e a revisão com gabarito — só da prova
     * entregue (em andamento: 422). O único endpoint que dá o gabarito ao aluno.
     */
    @GetMapping("/tentativas/{id}/resultado")
    public ResultadoResponse resultado(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResultadoResponse.de(resultados.detalhar(id, aluno(jwt)), questaoMapper);
    }

    private static UUID aluno(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
