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
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
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

    // Parâmetro B da TRI (microdados do INEP), carregado por --importar-habilidades.
    // Ainda não aparece na tela: a escala crua (~-3 a +4) não diz nada ao aluno
    private Double dificuldade;

    // Código da habilidade oficial (1–30), escrito pela carga --importar-habilidades.
    // É a coluna que a carga altera; a leitura com o texto vem de habilidadeOficial
    private Short habilidade;

    // A mesma coluna `habilidade`, junto com `area`, vista como relacionamento: a
    // FK composta da V8 aponta pra habilidade da PRÓPRIA área. Só leitura
    // (insertable/updatable = false) porque as duas colunas já são escritas pelos
    // campos acima — o JPA recusa duas fontes de escrita pra mesma coluna.
    // LAZY; o detalhe traz no mesmo SELECT via JOIN FETCH
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "area", referencedColumnName = "area", insertable = false, updatable = false),
            @JoinColumn(name = "habilidade", referencedColumnName = "codigo", insertable = false, updatable = false)
    })
    private Habilidade habilidadeOficial;

    @Column(nullable = false)
    private boolean anulada = false;

    // A fonte não trouxe o texto-base ou a imagem de que a pergunta depende (V9).
    // Escrita pela carga --marcar-incompletas
    @Column(nullable = false)
    private boolean incompleta = false;

    // NOT anulada AND NOT incompleta, calculada pelo PRÓPRIO banco (coluna gerada
    // da V9). Só leitura: o Postgres recusa escrita numa coluna gerada, então o
    // Hibernate não pode incluí-la em INSERT nem UPDATE. As consultas filtram por
    // ela; este campo existe pra quem precisar ler a regra numa entidade carregada
    @Column(insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private boolean disponivel;

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
