package br.com.simulados.api.simulado.api;

import br.com.simulados.api.questao.api.RotuloResponse;
import br.com.simulados.api.questao.domain.Area;
import br.com.simulados.api.simulado.application.ConsultaDeSimulado;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * O simulado pro dono: o que é e do que é feito. Sem as questões (a prova abre
 * pela tentativa, 3b — vê-las antes deixaria ler a prova com o cronômetro parado)
 * e sem gabarito.
 *
 * `total` é o tamanho da lista do simulado, não a soma da composição: questão sem
 * assunto não entraria no JOIN da composição, e o total tem que ser o da prova.
 */
public record SimuladoResponse(
        UUID id,
        String titulo,
        int total,
        Integer tempoLimiteSeg,
        Instant criadoEm,
        List<ItemDaComposicao> composicao
) {

    public record ItemDaComposicao(Area area, RotuloResponse materia, RotuloResponse assunto, long questoes) {
    }

    static SimuladoResponse de(ConsultaDeSimulado.Detalhe detalhe) {
        var simulado = detalhe.simulado();
        return new SimuladoResponse(simulado.getId(), simulado.getTitulo(), simulado.getQuestoes().size(),
                simulado.getTempoLimiteSeg(), simulado.getCriadoEm(),
                detalhe.composicao().stream()
                        .map(l -> new ItemDaComposicao(l.area(), new RotuloResponse(l.materiaSlug(), l.materiaNome()),
                                new RotuloResponse(l.assuntoSlug(), l.assuntoNome()), l.questoes()))
                        .toList());
    }
}
