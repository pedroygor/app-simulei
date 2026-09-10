package br.com.simulados.api.usuario.application;

import br.com.simulados.api.usuario.domain.TentativasDeLoginExcedidasException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trava o login depois de N falhas seguidas na MESMA conta.
 *
 * Por que por e-mail e não por IP: num cursinho a turma inteira sai pelo mesmo
 * IP (NAT). Limitar por IP faria o quinto aluno que errasse a senha bloquear a
 * sala toda — e esse é justamente o cenário de uso do produto. Limitar por
 * conta protege quem está sendo atacado sem punir quem não tem nada a ver.
 *
 * O contador vive em memória, o que basta enquanto houver uma instância só.
 * Com mais de uma, cada uma teria a própria contagem e o teto efetivo
 * multiplicaria — aí isto precisa virar Redis (ver comentário no fim).
 */
@Component
public class LimitadorDeTentativasDeLogin {

    static final int MAXIMO_DE_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    /** Acima disso, faz faxina antes de aceitar entrada nova. */
    private static final int LIMITE_ANTES_DA_FAXINA = 10_000;

    private record Contagem(int falhas, Instant bloqueadoAte) {}

    private final Map<String, Contagem> porConta = new ConcurrentHashMap<>();
    private final Clock clock;

    // Clock injetado pra o teste poder avançar o tempo sem esperar 15 minutos
    LimitadorDeTentativasDeLogin(Clock clock) {
        this.clock = clock;
    }

    /** Lança se a conta estiver bloqueada. Chamar ANTES de verificar a senha. */
    public void verificar(String email) {
        var contagem = porConta.get(chave(email));
        if (contagem == null || contagem.bloqueadoAte() == null) {
            return;
        }
        var agora = clock.instant();
        if (agora.isBefore(contagem.bloqueadoAte())) {
            throw new TentativasDeLoginExcedidasException(Duration.between(agora, contagem.bloqueadoAte()));
        }
        // Bloqueio venceu: zera e deixa tentar de novo
        porConta.remove(chave(email));
    }

    public void registrarFalha(String email) {
        if (porConta.size() > LIMITE_ANTES_DA_FAXINA) {
            removerExpirados();
        }
        porConta.compute(chave(email), (ignorado, atual) -> {
            var falhas = (atual == null ? 0 : atual.falhas()) + 1;
            var bloqueio = falhas >= MAXIMO_DE_FALHAS ? clock.instant().plus(JANELA) : null;
            return new Contagem(falhas, bloqueio);
        });
    }

    /** Login deu certo: a conta volta ao zero. */
    public void registrarSucesso(String email) {
        porConta.remove(chave(email));
    }

    // Normaliza pra o atacante não driblar variando maiúsculas
    private String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void removerExpirados() {
        var agora = clock.instant();
        porConta.values().removeIf(c -> c.bloqueadoAte() != null && agora.isAfter(c.bloqueadoAte()));
    }
}
