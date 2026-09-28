package br.com.simulados.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Pública porque os testes de integração vivem nos pacotes de cada domínio
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		// Mesma versão do docker-compose.yml: testar contra outra versão do banco
		// pode mascarar diferenças de comportamento
		return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
	}

}
