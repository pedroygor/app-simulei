package br.com.simulados.api.simulado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A EXECUÇÃO de um simulado por um aluno. As respostas não são uma coleção daqui:
 * responder grava UMA linha por upsert (ver RespostaRepository), e carregar até
 * 180 respostas pra mudar uma seria desperdício.
 *
 * Sem setters: o único jeito de mudar o estado é finalizar(), que guarda a regra.
 */
@Entity
@Table(name = "tentativas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tentativa {

    @Id
    @UuidGenerator
    private UUID id;

    // Ids e não entidades, como em Simulado.criadoPor: a tentativa só precisa
    // saber de quem é e de qual prova, nunca dos dados deles
    @Column(name = "simulado_id", nullable = false, updatable = false)
    private UUID simuladoId;

    @Column(name = "aluno_id", nullable = false, updatable = false)
    private UUID alunoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDaTentativa status;

    @CreationTimestamp
    @Column(name = "iniciado_em", nullable = false, updatable = false)
    private Instant iniciadoEm;

    @Column(name = "finalizado_em")
    private Instant finalizadoEm;

    // Nasce em andamento: sem turma (Etapa 5), criar a tentativa É iniciar a prova
    public Tentativa(UUID simuladoId, UUID alunoId) {
        this.simuladoId = simuladoId;
        this.alunoId = alunoId;
        this.status = StatusDaTentativa.EM_ANDAMENTO;
    }

    public boolean finalizada() {
        return status == StatusDaTentativa.FINALIZADA;
    }

    /** Pra quem vai escrever na tentativa: responder, desmarcar, finalizar. */
    public void exigirEmAndamento() {
        if (finalizada()) {
            throw new TentativaJaFinalizadaException();
        }
    }

    public void finalizar(Instant agora) {
        exigirEmAndamento();
        this.status = StatusDaTentativa.FINALIZADA;
        this.finalizadoEm = agora;
    }
}
