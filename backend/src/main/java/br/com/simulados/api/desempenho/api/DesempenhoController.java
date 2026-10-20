package br.com.simulados.api.desempenho.api;

import br.com.simulados.api.desempenho.application.ConsultaDeDesempenho;
import br.com.simulados.api.desempenho.application.EvolucaoDoDesempenho;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DesempenhoController {

    private final ConsultaDeDesempenho consulta;
    private final EvolucaoDoDesempenho evolucao;

    /**
     * O desempenho de quem está logado. Sem parâmetro nem corpo de propósito: o aluno
     * vem do `sub` do token, então não existe id vindo do cliente pra validar — nem
     * jeito de pedir o desempenho de outra pessoa.
     */
    @GetMapping("/desempenho")
    public DesempenhoResponse meu(@AuthenticationPrincipal Jwt jwt) {
        return DesempenhoResponse.de(consulta.doAluno(UUID.fromString(jwt.getSubject())));
    }

    /** O aproveitamento por semana, geral e por área. `?semanas=` de 1 a 52 (padrão 4). */
    @GetMapping("/desempenho/evolucao")
    public EvolucaoResponse evolucao(@Valid FiltroDaEvolucao filtro, @AuthenticationPrincipal Jwt jwt) {
        return EvolucaoResponse.de(evolucao.doAluno(UUID.fromString(jwt.getSubject()), filtro.semanasOuPadrao()));
    }
}
