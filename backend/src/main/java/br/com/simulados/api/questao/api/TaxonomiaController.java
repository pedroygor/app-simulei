package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.application.ConsultaDaTaxonomia;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/taxonomia")
@RequiredArgsConstructor
public class TaxonomiaController {

    private final ConsultaDaTaxonomia consultaDaTaxonomia;

    /**
     * Áreas → matérias → assuntos, com a contagem de questões de cada assunto.
     *
     * Exige login como o resto da API (regra geral do SecurityConfig). Deixar
     * pública pra landing do SaaS mostrar "118 assuntos" é decisão da Etapa 6.
     *
     * `?anos=` opcional: conta só as questões desses anos (tela de montar prova).
     * O @Valid valida o record de filtro montado a partir dos query params.
     */
    @GetMapping
    public List<AreaDaTaxonomiaResponse> listar(@Valid FiltroDaTaxonomia filtro) {
        return AreaDaTaxonomiaResponse.montar(consultaDaTaxonomia.listar(filtro.anosOuTodos()));
    }
}
