package br.com.simulados.api.questao.infrastructure;

import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.domain.Questao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

    // Chave natural da fonte — o job de importação usa pra ser idempotente
    // (rodar duas vezes não duplica questão)
    boolean existsByAnoAndNumeroAndIdioma(Integer ano, Integer numero, Idioma idioma);
}
