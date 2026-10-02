package br.com.simulados.api.simulado.infrastructure;

import br.com.simulados.api.simulado.domain.Resposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RespostaRepository extends JpaRepository<Resposta, Resposta.Chave> {

    /**
     * Responde ou troca a resposta, numa instrução atômica: ON CONFLICT na PK
     * transforma o INSERT em UPDATE quando a questão já foi respondida. Ver o
     * porquê de não usar save() na Resposta.
     *
     * Quem chama já conferiu que a questão é do simulado e calculou `correta`; a
     * FK composta da V11 ainda recusa alternativa de outra questão como backstop.
     *
     * @Modifying: avisa o Spring Data que a query escreve (executeUpdate em vez de
     * getResultList). flush/clearAutomatically: grava o que estiver pendente antes
     * e esvazia o contexto depois — senão uma Resposta já carregada continuaria
     * com o valor antigo em memória até o fim da transação.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO respostas (tentativa_id, questao_id, alternativa_id, correta)
            VALUES (:tentativa, :questao, :alternativa, :correta)
            ON CONFLICT (tentativa_id, questao_id) DO UPDATE
               SET alternativa_id = EXCLUDED.alternativa_id,
                   correta        = EXCLUDED.correta,
                   respondida_em  = now()
            """, nativeQuery = true)
    void registrar(@Param("tentativa") UUID tentativa,
                   @Param("questao") Long questao,
                   @Param("alternativa") Long alternativa,
                   @Param("correta") boolean correta);

    // Desmarcar = a questão volta a ficar em branco, isto é, sem linha. Nativo
    // porque o Hibernate recusa DELETE em HQL sobre entidade @Immutable
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM respostas WHERE tentativa_id = :tentativa AND questao_id = :questao",
            nativeQuery = true)
    void apagar(@Param("tentativa") UUID tentativa, @Param("questao") Long questao);

    List<Resposta> findByTentativaId(UUID tentativaId);
}
