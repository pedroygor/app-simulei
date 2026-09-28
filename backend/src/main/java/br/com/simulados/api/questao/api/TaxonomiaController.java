package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.application.ConsultaDaTaxonomia;
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
     * Sem parâmetro de entrada, não há o que validar.
     */
    @GetMapping
    public List<AreaDaTaxonomiaResponse> listar() {
        return AreaDaTaxonomiaResponse.montar(consultaDaTaxonomia.listar());
    }
}
