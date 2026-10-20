package br.com.simulados.api.simulado.application;

import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.questao.domain.Assunto;
import br.com.simulados.api.questao.domain.Idioma;
import br.com.simulados.api.questao.infrastructure.AssuntoRepository;
import br.com.simulados.api.simulado.application.DistribuidorDeQuestoes.Cota;
import br.com.simulados.api.simulado.application.PedidoDeSimulado.Escolha;
import br.com.simulados.api.simulado.domain.Simulado;
import br.com.simulados.api.simulado.infrastructure.SimuladoRepository;
import br.com.simulados.api.simulado.infrastructure.SorteadorDeQuestoes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Monta uma prova a partir das escolhas do usuário (Etapa 3a do CLAUDE.md):
 *
 * 1. expande cada escolha em assuntos (Matemática = os assuntos dela), resolvendo
 *    a língua estrangeira pelo `idioma` do pedido;
 * 2. recusa escolha inexistente e escolhas sobrepostas;
 * 3. CONTA as disponíveis por assunto (1 query);
 * 4. DISTRIBUI o total entre as escolhas (Java puro, DistribuidorDeQuestoes);
 * 5. SORTEIA tudo (1 query) e salva.
 *
 * Número fixo de queries — taxonomia, contagem, sorteio, INSERTs — com 1 escolha
 * ou com 30.
 */
@Service
@RequiredArgsConstructor
public class CriacaoDeSimulado {

    // Matérias de língua estrangeira (slugs da V5): só entram numa área inteira se
    // o pedido escolher o idioma. A prova nunca mistura as duas versões da questão
    private static final Map<Idioma, String> MATERIA_DO_IDIOMA = Map.of(
            Idioma.INGLES, "ingles",
            Idioma.ESPANHOL, "espanhol");

    private final AssuntoRepository assuntoRepository;
    private final SorteadorDeQuestoes sorteador;
    private final SimuladoRepository simuladoRepository;

    /**
     * @Transactional: contar, sortear e salvar formam uma operação só. Se o INSERT
     * falhar, nada fica pela metade. readOnly não: no fim há escrita.
     *
     * `dono` vem do token (o controller tira do JWT), nunca do corpo do pedido.
     */
    @Transactional
    public SimuladoCriado criar(UUID dono, PedidoDeSimulado pedido) {
        var anos = pedido.anos() == null ? List.<Integer>of()
                : pedido.anos().stream().distinct().sorted().toList();

        var expandidas = expandir(pedido.escolhas(), pedido.idioma());
        recusarSobreposicao(expandidas);

        var todosOsAssuntos = new HashSet<Integer>();
        expandidas.forEach(e -> todosOsAssuntos.addAll(e.assuntos()));
        var contagem = sorteador.contarPorAssunto(todosOsAssuntos, anos);

        var cotas = expandidas.stream()
                .map(e -> new Cota(e.rotulo(), e.quantidade(),
                        e.assuntos().stream().mapToLong(a -> contagem.getOrDefault(a, 0L)).sum()))
                .toList();
        // ThreadLocalRandom: o service é um singleton chamado por várias threads ao
        // mesmo tempo, e um gerador compartilhado não é seguro entre threads
        var quantidades = new DistribuidorDeQuestoes(ThreadLocalRandom.current()).distribuir(pedido.total(), cotas);

        var grupoDoAssunto = new HashMap<Integer, Integer>();
        for (int g = 0; g < expandidas.size(); g++) {
            for (var assunto : expandidas.get(g).assuntos()) {
                grupoDoAssunto.put(assunto, g);
            }
        }
        var questoes = sorteador.sortear(dono, grupoDoAssunto, quantidades, anos);

        var simulado = simuladoRepository.save(
                new Simulado(dono, pedido.titulo().strip(), pedido.tempoLimiteSeg(), questoes));

        var composicao = new ArrayList<SimuladoCriado.Parte>();
        for (int g = 0; g < expandidas.size(); g++) {
            composicao.add(new SimuladoCriado.Parte(expandidas.get(g).rotulo(), quantidades.get(g)));
        }
        return new SimuladoCriado(simulado, composicao);
    }

    /** Uma escolha do pedido traduzida em assuntos, com o nome pra mensagem. */
    private record Expandida(String rotulo, Integer quantidade, Set<Integer> assuntos) {
    }

    private List<Expandida> expandir(List<Escolha> escolhas, Idioma idioma) {
        // Taxonomia inteira numa query (118 linhas, com a matéria no mesmo SELECT)
        var taxonomia = assuntoRepository.buscarTodosComMateria();
        var lingua = idioma == null ? null : MATERIA_DO_IDIOMA.get(idioma);

        var expandidas = new ArrayList<Expandida>();
        for (var escolha : escolhas) {
            var valor = escolha.valor().strip();
            var expandida = switch (escolha.tipo()) {
                case AREA -> {
                    var area = area(valor);
                    var assuntos = ids(taxonomia.stream()
                            .filter(a -> a.getArea() == area)
                            .filter(a -> !ehLinguaEstrangeira(a) || a.getMateria().getSlug().equals(lingua))
                            .toList());
                    yield new Expandida(area.getNome(), escolha.quantidade(), assuntos);
                }
                case MATERIA -> {
                    // Matéria escolhida explicitamente (inclusive Inglês) entra inteira
                    var daMateria = taxonomia.stream().filter(a -> a.getMateria().getSlug().equals(valor)).toList();
                    if (daMateria.isEmpty()) {
                        throw new PedidoDeSimuladoInvalidoException("'%s' não é uma matéria da taxonomia".formatted(valor));
                    }
                    yield new Expandida(daMateria.getFirst().getMateria().getNome(), escolha.quantidade(), ids(daMateria));
                }
                case ASSUNTO -> {
                    var assunto = taxonomia.stream().filter(a -> a.getSlug().equals(valor)).findFirst()
                            .orElseThrow(() -> new PedidoDeSimuladoInvalidoException(
                                    "'%s' não é um assunto da taxonomia".formatted(valor)));
                    yield new Expandida(assunto.getNome(), escolha.quantidade(), Set.of(assunto.getId()));
                }
            };
            expandidas.add(expandida);
        }
        return expandidas;
    }

    /**
     * Nenhum assunto pode estar em duas escolhas: a mesma questão poderia sair duas
     * vezes, e "Matemática (10) + Probabilidade (5)" é ambíguo. Recusa nomeando o
     * par, pra quem montou saber o que tirar.
     */
    private void recusarSobreposicao(List<Expandida> expandidas) {
        for (int i = 0; i < expandidas.size(); i++) {
            for (int j = i + 1; j < expandidas.size(); j++) {
                var a = expandidas.get(i);
                var b = expandidas.get(j);
                if (a.assuntos().stream().anyMatch(b.assuntos()::contains)) {
                    // A menor está dentro da maior ("Probabilidade" dentro de "Matemática")
                    var menor = a.assuntos().size() <= b.assuntos().size() ? a : b;
                    var maior = menor == a ? b : a;
                    throw new PedidoDeSimuladoInvalidoException(menor.rotulo().equals(maior.rotulo())
                            ? "%s aparece mais de uma vez nas escolhas".formatted(menor.rotulo())
                            : "%s já está dentro de %s: escolha um dos dois".formatted(menor.rotulo(), maior.rotulo()));
                }
            }
        }
    }

    private static Area area(String valor) {
        try {
            return Area.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoDeSimuladoInvalidoException("'%s' não é uma área (use LINGUAGENS, CIENCIAS_HUMANAS, CIENCIAS_NATUREZA ou MATEMATICA)"
                    .formatted(valor));
        }
    }

    private static boolean ehLinguaEstrangeira(Assunto assunto) {
        return MATERIA_DO_IDIOMA.containsValue(assunto.getMateria().getSlug());
    }

    private static Set<Integer> ids(List<Assunto> assuntos) {
        var ids = new LinkedHashSet<Integer>();
        assuntos.forEach(a -> ids.add(a.getId()));
        return ids;
    }
}
