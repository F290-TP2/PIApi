package br.com.fatecararas.piapi.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI propostasOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API de propostas interdisciplinares")
                .version("1.0.0")
                .description("API didática da Fatec Antonio Brambilla. "
                        + "Listagens públicas contêm somente propostas publicadas, com até 10 itens por página. "
                        + "Nesta etapa não há autenticação: autor e curtidas usam o usuário demonstrativo. "
                        + "Os dados do H2 em memória são apagados ao encerrar a aplicação."));
    }
}
