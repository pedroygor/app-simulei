package br.com.simulados.api.questao.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questoes")
@Getter
@Setter
@NoArgsConstructor
public class Questao {

    // IDENTITY: o banco gera o id no INSERT (coluna GENERATED AS IDENTITY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    private Idioma idioma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Area area;

    // Um assunto principal (a matéria vem dele — ver Assunto). Nullable:
    // questão ainda não classificada. LAZY pela regra da seção 8.4; a busca e
    // o detalhe trazem assunto e matéria no mesmo SELECT via JOIN FETCH
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assunto_id")
    private Assunto assunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_origem")
    private OrigemDaClassificacao classificacaoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_confianca")
    private ConfiancaDaClassificacao classificacaoConfianca;

    // Nullable: 5% das questões da enem.dev não têm texto-base — a pergunta
    // inteira fica no `comando`. CHECK no banco garante que um dos dois exista.
    private String enunciado;

    private String comando;

    @Column(name = "imagem_url")
    private String imagemUrl;

    private String gabarito;

    private Double dificuldade;

    private Short habilidade;

    @Column(nullable = false)
    private boolean anulada = false;

    @Column(nullable = false)
    private String fonte;

    @Column(name = "fonte_id", nullable = false)
    private String fonteId;

    // cascade + orphanRemoval: alternativa vive e morre com a questão —
    // salvar a questão persiste a lista, nunca se salva alternativa avulsa
    @OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("letra")
    private List<Alternativa> alternativas = new ArrayList<>();

    // Mantém os dois lados do relacionamento coerentes; sem isso o
    // questao_id iria nulo no INSERT da alternativa
    public void adicionarAlternativa(Alternativa alternativa) {
        alternativa.setQuestao(this);
        alternativas.add(alternativa);
    }
}
