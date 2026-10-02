package br.com.simulados.api.simulado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Uma questão dentro de um simulado, com a posição dela na prova.
 *
 * @Embeddable: não é entidade (não tem identidade nem ciclo de vida próprios);
 * é um VALOR que só existe dentro de um Simulado. Vira uma linha de
 * simulado_questoes pelo @ElementCollection do Simulado.
 *
 * Guarda o id da questão, não a entidade Questao: o simulado só precisa saber
 * QUAIS questões tem. Quem mostrar a prova (3b) busca as questões numa query só,
 * com as alternativas — referenciar a entidade aqui convidaria o N+1 de carregar
 * questão por questão ao percorrer a lista.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // o JPA precisa; código nosso não
@AllArgsConstructor
@EqualsAndHashCode
public class QuestaoDoSimulado {

    @Column(name = "questao_id", nullable = false)
    private Long questaoId;

    @Column(nullable = false)
    private Short ordem;
}
