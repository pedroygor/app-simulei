package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.simulado.domain.LinhaDaComposicao;
import br.com.simulados.api.simulado.domain.Simulado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimuladoRepository extends JpaRepository<Simulado, UUID> {

    /**
     * O simulado SÓ se for do usuário. A autorização por dono mora na própria query
     * (seção 8.1): não existe "buscar e depois conferir o dono", em que um
     * esquecimento no service vazaria o simulado alheio.
     *
     * LEFT JOIN FETCH da lista de questões (@ElementCollection, LAZY): sem ele, ler
     * o tamanho da lista pro total da resposta dispararia uma 2ª query. Tentei antes
     * um @EntityGraph numa query derivada, e o Hibernate o ignorou pra coleção de
     * valores — o JOIN FETCH explícito não depende de dica nenhuma.
     */
    @Query("""
            SELECT s FROM Simulado s
            LEFT JOIN FETCH s.questoes
            WHERE s.id = :id AND s.criadoPor = :usuario
            """)
    Optional<Simulado> buscarDoUsuario(@Param("id") UUID id, @Param("usuario") UUID usuario);

    /**
     * O simulado com a ordem das questões, SEM filtro de dono: só pra quem já
     * autorizou por outro caminho — a prova de uma tentativa que é do aluno. Na
     * Etapa 5 o aluno faz a prova da turma, cujo dono é o professor.
     */
    @Query("""
            SELECT s FROM Simulado s
            LEFT JOIN FETCH s.questoes
            WHERE s.id = :id
            """)
    Optional<Simulado> buscarComQuestoes(@Param("id") UUID id);

    /**
     * Quantas questões do simulado há em cada assunto, num GROUP BY só. Join pelo
     * id da questão guardado no @ElementCollection (o simulado não mapeia a
     * entidade Questao de propósito — ver QuestaoDoSimulado). Na ordem da taxonomia.
     */
    @Query("""
            SELECT new br.com.simulados.api.simulado.domain.LinhaDaComposicao(
                       q.area, m.slug, m.nome, a.slug, a.nome, count(q))
            FROM Simulado s
            JOIN s.questoes sq
            JOIN Questao q ON q.id = sq.questaoId
            JOIN q.assunto a
            JOIN a.materia m
            WHERE s.id = :id
            GROUP BY q.area, m.slug, m.nome, m.ordem, a.slug, a.nome, a.ordem
            ORDER BY m.ordem, a.ordem
            """)
    List<LinhaDaComposicao> composicao(@Param("id") UUID id);

    /**
     * Se a alternativa marcada está certa — e, de quebra, se a resposta faz
     * sentido: só devolve linha quando a questão está NESTE simulado e a
     * alternativa é DESTA questão. Vazio = resposta inválida. Validação e correção
     * numa query só, em vez de três buscas.
     *
     * CASE e não "a.letra = q.gabarito" direto: o JPQL não aceita comparação como
     * valor no SELECT.
     *
     * `disponivel`: questão anulada ou incompleta depois da montagem saiu da prova
     * (ver QuestaoRepository.buscarVariasComAlternativas), então responder a ela
     * é resposta inválida, como uma questão de fora do simulado.
     */
    @Query("""
            SELECT CASE WHEN a.letra = q.gabarito THEN true ELSE false END
            FROM Simulado s
            JOIN s.questoes sq
            JOIN Questao q ON q.id = sq.questaoId
            JOIN q.alternativas a
            WHERE s.id = :simulado AND q.id = :questao AND a.id = :alternativa
              AND q.disponivel = true
            """)
    Optional<Boolean> alternativaEstaCorreta(@Param("simulado") UUID simulado,
                                             @Param("questao") Long questao,
                                             @Param("alternativa") Long alternativa);
}
