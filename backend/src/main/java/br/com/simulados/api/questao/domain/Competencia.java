package br.com.simulados.api.questao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;

/**
 * Competência de área da Matriz de Referência do ENEM (V8). O número só é único
 * DENTRO da área — a C5 de Matemática não é a C5 de Natureza —, por isso a chave
 * é composta.
 *
 * @IdClass: diz ao JPA que a chave é o par (area, numero), declarado na classe Id
 * abaixo. Imutável pelo mesmo motivo de Materia: dado de referência, só leitura.
 */
@Entity
@Table(name = "competencias")
@IdClass(Competencia.Id.class)
@Immutable
@Getter
@NoArgsConstructor
public class Competencia {

    @jakarta.persistence.Id
    @Enumerated(EnumType.STRING)
    private Area area;

    @jakarta.persistence.Id
    private Short numero;

    @Column(nullable = false)
    private String descricao;

    // A classe de chave do @IdClass precisa de equals/hashCode (o Hibernate a usa
    // como chave de mapa no contexto de persistência) e de ser Serializable
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        private Area area;
        private Short numero;
    }
}
