package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.domain.ResumoDeQuestao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestaoMapper {

    QuestaoResumoResponse toResumo(ResumoDeQuestao resumo);

    // @Mapping com caminho aninhado: o MapStruct gera a navegação
    // questao.getAssunto().getMateria().getNome() já com checagem de null em
    // cada passo — questão sem classificação devolve matéria e assunto nulos
    // em vez de NullPointerException
    @Mapping(target = "materia", source = "assunto.materia.nome")
    @Mapping(target = "assunto", source = "assunto.nome")
    QuestaoDetalheResponse toDetalhe(Questao questao);

    AlternativaResponse toAlternativa(Alternativa alternativa);

    List<AlternativaResponse> toAlternativas(List<Alternativa> alternativas);
}
