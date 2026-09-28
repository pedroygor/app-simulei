package br.com.simulados.api.questao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Assunto de uma questão ("probabilidade", "função do 1º grau"). Cada questão
 * tem UM assunto principal — ver docs/taxonomia.md e o ADR de 25/09/2026.
 *
 * Imutável pelo mesmo motivo de Materia: dado de referência, só leitura.
 */
@Entity
@Table(name = "assuntos")
@Immutable
@Getter
@NoArgsConstructor
public class Assunto {

    @Id
    private Integer id;

    // LAZY: a maioria das leituras de assunto não precisa da matéria. Quando
    // precisa (busca, detalhe), a query traz as duas num JOIN FETCH
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    // Cópia da área da matéria, mantida pelo banco (FK composta). Existe pra
    // o banco recusar assunto de outra área numa questão — ver V4
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Area area;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String nome;

    @Column(name = "grupo_matriz", nullable = false)
    private String grupoMatriz;
}
