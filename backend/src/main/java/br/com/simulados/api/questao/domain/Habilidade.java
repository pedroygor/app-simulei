package br.com.simulados.api.questao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;

/**
 * Habilidade oficial da Matriz de Referência do ENEM (H1–H30 em cada área, V8).
 *
 * Não substitui o assunto: o assunto ("probabilidade") é a nossa taxonomia e é
 * o que organiza a prova e o diagnóstico; a habilidade é a referência oficial do
 * INEP, que o professor reconhece. Ver docs/habilidade-x-assunto.md.
 *
 * Chave composta (area, codigo) pelo mesmo motivo da Competencia.
 */
@Entity
@Table(name = "habilidades")
@IdClass(Habilidade.Id.class)
@Immutable
@Getter
@NoArgsConstructor
public class Habilidade {

    @jakarta.persistence.Id
    @Enumerated(EnumType.STRING)
    private Area area;

    @jakarta.persistence.Id
    private Short codigo;

    // LAZY pela regra da seção 8.4; o detalhe da questão traz as duas num JOIN FETCH.
    // As duas colunas da FK composta são lidas, nunca escritas daqui: `area` já
    // pertence à chave desta entidade
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "area", referencedColumnName = "area", insertable = false, updatable = false),
            @JoinColumn(name = "competencia", referencedColumnName = "numero", insertable = false, updatable = false)
    })
    private Competencia competencia;

    @Column(nullable = false)
    private String descricao;

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        private Area area;
        private Short codigo;
    }
}
