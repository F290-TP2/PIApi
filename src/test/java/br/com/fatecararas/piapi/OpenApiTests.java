package br.com.fatecararas.piapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:openapi-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class OpenApiTests {

    @Autowired MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void documentaOperacoesSemInventarCorpoNo204OuExporEntidades() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de propostas interdisciplinares"))
                .andExpect(jsonPath("$.paths['/api/propostas'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/api/propostas'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/propostas/{id}'].delete.responses['204'].content").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/categorias'].get.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/propostas'].post.responses['400'].content['application/json'].examples.Validacao").exists())
                .andExpect(jsonPath("$.components.schemas.CriarPropostaRequest.required", hasSize(6)))
                .andExpect(jsonPath("$.components.schemas.CriarPropostaRequest.properties.titulo.minLength").value(5))
                .andExpect(jsonPath("$.components.schemas.CriarPropostaRequest.properties.titulo.maxLength").value(120))
                .andReturn().getResponse().getContentAsString();
        JsonNode document = json.readTree(body);
        assertThat(document.path("paths").size()).isEqualTo(6);
        HashSet<String> operationIds = new HashSet<>();
        int operations = 0;
        for (JsonNode path : document.path("paths")) {
            for (JsonNode operation : path) {
                String id = operation.path("operationId").asText();
                assertThat(id).isNotBlank();
                assertThat(operationIds.add(id)).as("operationId único: %s", id).isTrue();
                operations++;
            }
        }
        assertThat(operations).isEqualTo(10);
        assertThat(body).doesNotContain("senhaHash", "#/components/schemas/Usuario", "\"/error\"");
        String pageRef = document.at("/paths/~1api~1propostas/get/responses/200/content/application~1json/schema/$ref").asText();
        assertThat(pageRef).startsWith("#/components/schemas/");
        JsonNode page = document.at(pageRef.substring(1));
        assertThat(page.at("/properties/conteudo/items/$ref").asText()).isEqualTo("#/components/schemas/PropostaResponse");
    }

    @Test
    void disponibilizaInterfaceConfiguracaoEYaml() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/v3/api-docs"));
        mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("openapi:")));
    }

    @Test
    void respostasReaisCorrespondemAosCasosDocumentados() throws Exception {
        String request = """
                {"titulo":"Horta comunitária conectada","resumo":"Monitoramento local.",
                 "descricao":"Tecnologia para a comunidade.","publicoAlvoIds":[1],"categoriaId":1,"cursoIds":[1,2]}
                """;
        String created = mvc.perform(post("/api/propostas").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).path("id").asLong();
        mvc.perform(post("/api/propostas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.titulo").exists());
        mvc.perform(post("/api/propostas").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos").isEmpty());
        mvc.perform(post("/api/propostas").contentType(MediaType.APPLICATION_JSON)
                        .content(request.replace("\"categoriaId\":1", "\"categoriaId\":999999")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos").isEmpty());
        mvc.perform(get("/api/propostas").param("page", "abc")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/propostas/{id}/curtida", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeCurtidas").value(1));
        mvc.perform(put("/api/propostas/{id}/curtida", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeCurtidas").value(1));
        mvc.perform(delete("/api/propostas/{id}/curtida", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeCurtidas").value(0));
        mvc.perform(delete("/api/propostas/{id}", id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get("/api/propostas/{id}", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
