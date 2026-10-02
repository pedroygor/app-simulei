package br.com.simulados.api.simulado.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A DEFINIÇÃO de uma prova: quem criou, título, tempo e as questões em ordem.
 * A execução por um aluno é a Tentativa (3b) — ver a decisão central na seção 6
 * do CLAUDE.md.
 *
 * Sem setters: um simulado nasce pronto (o sorteio da 3a escolhe as questões de
 * uma vez) e não muda depois. Editar a prova com tentativas já feitas
 * invalidaria a correção de quem já respondeu.
 */
@Entity
@Table(name = "simulados")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Simulado {

    // UUID gerado na aplicação, como em Usuario: o id existe antes do INSERT
    @Id
    @UuidGenerator
    private UUID id;

    // Id do dono, não a entidade Usuario: o simulado nunca precisa dos dados do
    // usuário, só de saber de quem é (autorização por dono, seção 8.1)
    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @Column(nullable = false)
    private String titulo;

    // null = sem limite de tempo
    @Column(name = "tempo_limite_seg")
    private Integer tempoLimiteSeg;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * @ElementCollection: coleção de VALORES (o @Embeddable acima) guardada numa
     * tabela à parte, simulado_questoes. O ciclo de vida é o do simulado: salvar o
     * simulado grava as linhas, e não existe "salvar uma questão do simulado"
     * avulsa. É LAZY por padrão — listar simulados não carrega as questões.
     *
     * @OrderBy("ordem"): o Hibernate devolve a lista na ordem da prova, por SQL.
     */
    @ElementCollection
    @CollectionTable(name = "simulado_questoes", joinColumns = @JoinColumn(name = "simulado_id"))
    @OrderBy("ordem")
    private List<QuestaoDoSimulado> questoes = new ArrayList<>();

    /**
     * A ordem vem da posição na lista recebida (1, 2, 3...): quem monta a prova
     * decide a sequência, e aqui ela só é numerada. Lista vazia ou com questão
     * repetida é recusada antes de virar violação de PK no banco.
     */
    public Simulado(UUID criadoPor, String titulo, Integer tempoLimiteSeg, List<Long> questoesEmOrdem) {
        if (questoesEmOrdem.isEmpty()) {
            throw new IllegalArgumentException("simulado sem questões");
        }
        if (questoesEmOrdem.stream().distinct().count() != questoesEmOrdem.size()) {
            throw new IllegalArgumentException("simulado com questão repetida");
        }
        this.criadoPor = criadoPor;
        this.titulo = titulo;
        this.tempoLimiteSeg = tempoLimiteSeg;
        for (int i = 0; i < questoesEmOrdem.size(); i++) {
            questoes.add(new QuestaoDoSimulado(questoesEmOrdem.get(i), (short) (i + 1)));
        }
    }

    public boolean pertenceA(UUID usuarioId) {
        return criadoPor.equals(usuarioId);
    }
}
