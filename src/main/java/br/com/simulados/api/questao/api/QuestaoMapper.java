package br.com.simulados.api.questao.api;

import br.com.simulados.api.questao.domain.Alternativa;
import br.com.simulados.api.questao.domain.Questao;
import br.com.simulados.api.questao.domain.ResumoDeQuestao;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestaoMapper {

    QuestaoResumoResponse toResumo(ResumoDeQuestao resumo);

    QuestaoDetalheResponse toDetalhe(Questao questao);

    AlternativaResponse toAlternativa(Alternativa alternativa);

    List<AlternativaResponse> toAlternativas(List<Alternativa> alternativas);
}
