package br.com.simulados.api.simulado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * A alternativa marcada numa questão da tentativa. Questão em branco não tem linha.
 *
 * @Immutable: pro Hibernate a entidade é só de LEITURA (sem dirty checking, sem
 * UPDATE). A escrita é um upsert nativo no RespostaRepository: o save() do JPA
 * com id definido faz SELECT e depois INSERT ou UPDATE, e dois toques rápidos na
 * bolha (duas requisições em paralelo) bateriam na PK. O INSERT ... ON CONFLICT
 * do Postgres faz as duas coisas numa instrução atômica.
 *
 * @IdClass: a PK é composta (tentativa_id, questao_id); a classe Chave abaixo é
 * o que o JPA usa como "id" dessa entidade.
 */
@Entity
@Table(name = "respostas")
@IdClass(Resposta.Chave.class)
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resposta {

    @Id
    @Column(name = "tentativa_id")
    private UUID tentativaId;

    @Id
    @Column(name = "questao_id")
    private Long questaoId;

    @Column(name = "alternativa_id", nullable = false)
    private Long alternativaId;

    // Nunca sai na API antes de a prova ser finalizada (a correção é da 3c)
    @Column(nullable = false)
    private boolean correta;

    @Column(name = "respondida_em", nullable = false)
    private Instant respondidaEm;

    // O JPA exige equals/hashCode e Serializable na classe de id composto
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Chave implements Serializable {
        private UUID tentativaId;
        private Long questaoId;
    }
}
