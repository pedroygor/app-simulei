package br.com.simulados.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		var app = new SpringApplication(ApiApplication.class);
		// Rodando como job de importação não precisamos de servidor web — e
		// tentar subir o Tomcat colidiria com a aplicação já rodando na 8080.
		// A decisão tem que ser aqui: quando o ApplicationRunner executa, o
		// servidor já subiu.
		if (ImportacaoArgs.ehJob(args)) {
			app.setWebApplicationType(WebApplicationType.NONE);
		}
		app.run(args);
	}

}
