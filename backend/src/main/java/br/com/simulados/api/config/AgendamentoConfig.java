package br.com.simulados.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o @Scheduled (o encerramento de provas vencidas). Só com a API web no ar:
 * nos comandos de carga (--importar-questoes etc., que sobem sem servidor web)
 * não há prova pra encerrar, e uma tarefa agendada ali seria só ruído.
 *
 * @EnableScheduling aqui não desliga autoconfiguração nenhuma do Boot (ao
 * contrário do @EnableSpringDataWebSupport): o Boot só CRIA o executor quando
 * vê o agendamento ligado.
 */
@Configuration
@ConditionalOnWebApplication
@EnableScheduling
public class AgendamentoConfig {
}
