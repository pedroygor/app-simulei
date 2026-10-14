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

import java.time.Duration;
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

    // Até quando dá pra responder; NULL = prova sem tempo-limite. Fixado ao
    // iniciar (ver TentativaRepository.criarSeNaoExistir), nunca muda depois
    @Column(updatable = false)
    private Instant prazo;

    /**
     * Folga depois do prazo pra resposta que o aluno tocou no último segundo e
     * ainda estava a caminho (rede lenta do cursinho). O job de encerramento
     * espera a mesma folga, pra não fechar a prova com essa resposta em voo.
     */
    public static final Duration TOLERANCIA = Duration.ofSeconds(10);

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

    /**
     * A prova acabou pelo TEMPO (o front entregou com o cronômetro zerado, ou o
     * job encerrou a abandonada): nos dois casos `finalizar` grava o próprio
     * prazo como hora da entrega. Entregue antes do prazo = acabou pelo aluno.
     */
    public boolean encerradaPeloTempo() {
        return prazo != null && finalizadoEm != null && !finalizadoEm.isBefore(prazo);
    }

    /** Pra quem vai RESPONDER: depois do prazo (e da folga), não aceita mais. */
    public void exigirDentroDoPrazo(Instant agora) {
        if (prazo != null && agora.isAfter(prazo.plus(TOLERANCIA))) {
            throw new TempoEsgotadoException();
        }
    }

    /**
     * Entregar é permitido mesmo depois do prazo — é o que o front faz quando o
     * cronômetro zera. Mas o horário gravado é no máximo o prazo: tempo além do
     * limite não é "tempo de prova" (e a 4c vai medir duração por ele).
     */
    public void finalizar(Instant agora) {
        exigirEmAndamento();
        this.status = StatusDaTentativa.FINALIZADA;
        this.finalizadoEm = prazo != null && agora.isAfter(prazo) ? prazo : agora;
    }
}
