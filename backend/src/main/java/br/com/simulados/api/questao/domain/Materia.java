package br.com.simulados.api.questao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Disciplina dentro de uma área do ENEM (Física, Química, História...).
 *
 * Dado de referência: nasce na migration V5 e a aplicação só lê. O
 * `@Immutable` do Hibernate diz exatamente isso — ele não guarda cópia da
 * entidade pra detectar alteração no fim da transação (dirty checking) e
 * nunca gera UPDATE nesta tabela. Por isso não há @Setter.
 */
@Entity
@Table(name = "materias")
@Immutable
@Getter
@NoArgsConstructor
public class Materia {

    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Area area;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String nome;

    // Posição na taxonomia (1..14). Existe porque o id NÃO é a ordem: ver V7
    @Column(nullable = false)
    private Integer ordem;
}
