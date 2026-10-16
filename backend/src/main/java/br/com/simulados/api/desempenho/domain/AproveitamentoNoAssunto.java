package br.com.simulados.api.desempenho.domain;

import br.com.simulados.api.questao.domain.Area;

/** O aproveitamento do aluno num assunto, com matéria e assunto já resolvidos pelo banco. */
public record AproveitamentoNoAssunto(
        Area area,
        String materiaSlug,
        String materiaNome,
        String assuntoSlug,
        String assuntoNome,
        Aproveitamento aproveitamento
) {
}
