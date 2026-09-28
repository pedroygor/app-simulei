package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.Materia;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.domain.ResumoDeQuestao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestaoMapper {

    /**
     * Escrito à mão, não gerado: a projeção traz matéria e assunto achatados
     * (slug e nome em campos separados), e o MapStruct, ao montar o objeto
     * aninhado, criaria um {slug: null, nome: null} pra questão não
     * classificada. O contrato certo é `null` — "não tem assunto", e não
     * "tem um assunto sem nome".
     */
    default QuestaoResumoResponse toResumo(ResumoDeQuestao resumo) {
        return new QuestaoResumoResponse(
                resumo.id(), resumo.ano(), resumo.numero(), resumo.area(), resumo.idioma(),
                rotulo(resumo.materiaSlug(), resumo.materiaNome()),
                rotulo(resumo.assuntoSlug(), resumo.assuntoNome()),
                resumo.previa());
    }

    private static RotuloResponse rotulo(String slug, String nome) {
        return slug == null ? null : new RotuloResponse(slug, nome);
    }

    // Aqui o MapStruct resolve sozinho: a origem é a própria entidade
    // Assunto, e os métodos toRotulo abaixo devolvem null quando ela é nula
    // (ele gera a checagem). A navegação assunto.materia é null-safe também
    @Mapping(target = "materia", source = "assunto.materia")
    @Mapping(target = "assunto", source = "assunto")
    QuestaoDetalheResponse toDetalhe(Questao questao);

    RotuloResponse toRotulo(Materia materia);

    RotuloResponse toRotulo(Assunto assunto);

    AlternativaResponse toAlternativa(Alternativa alternativa);

    List<AlternativaResponse> toAlternativas(List<Alternativa> alternativas);
}
