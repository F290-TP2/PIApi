package br.com.fatecararas.piapi;

import br.com.fatecararas.piapi.api.dto.CriarPropostaRequest;
import br.com.fatecararas.piapi.config.DadosIniciais;
import br.com.fatecararas.piapi.domain.*;
import br.com.fatecararas.piapi.domain.entity.*;
import br.com.fatecararas.piapi.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PersistenciaMongoTests {
    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO::getConnectionString);
        registry.add("spring.mongodb.database", () -> "piapi_test");
    }

    static final String CATEGORIA = "000000000001000000000001";
    static final String CURSO = "000000000002000000000001";
    static final String PUBLICO = "000000000003000000000001";

    @Autowired PropostaService service;
    @Autowired PropostaRepository propostas;
    @Autowired CurtidaRepository curtidas;
    @Autowired CategoriaRepository categorias;
    @Autowired CursoRepository cursos;
    @Autowired UsuarioRepository usuarios;
    @Autowired PublicoAlvoRepository publicos;
    @Autowired DadosIniciais dados;
    @Autowired MongoTemplate mongo;
    @Autowired MockMvc mvc;

    @BeforeEach
    void preparar() {
        // deleteAll preserva os índices; não apagar o banco/coleções entre testes.
        curtidas.deleteAll();
        propostas.deleteAll();
        categorias.deleteAll();
        cursos.deleteAll();
        usuarios.deleteAll();
        publicos.deleteAll();
        dados.run(null);
    }

    private CriarPropostaRequest request(String titulo) {
        return new CriarPropostaRequest(titulo, "Resumo comunitário", "Descrição da proposta",
                List.of(PUBLICO), CATEGORIA, List.of(CURSO));
    }

    @Test
    void documentosReferenciasEHistorico() {
        var criada = service.criar(request("Horta conectada"));
        Proposta lida = propostas.findById(criada.id()).orElseThrow();
        assertThat(lida.getCategoriaId()).isEqualTo(CATEGORIA);
        assertThat(lida.getCursoIds()).containsExactly(CURSO);
        assertThat(lida.getPublicoAlvoIds()).containsExactly(PUBLICO);
        assertThat(lida.getCriadaEm()).isNotNull();
        assertThat(lida.getAtualizadaEm()).isNotNull();
        assertThat(mongo.getCollection("propostas").find().first().get("_id"))
                .isInstanceOf(org.bson.types.ObjectId.class);
        var atualizada = service.atualizar(criada.id(), new br.com.fatecararas.piapi.api.dto.AtualizarPropostaRequest(
                "Horta atualizada", "Outro resumo", "Outra descrição", List.of(PUBLICO), CATEGORIA, List.of(CURSO)));
        assertThat(atualizada.criadaEm()).isEqualTo(lida.getCriadaEm());
        assertThat(atualizada.atualizadaEm()).isAfterOrEqualTo(lida.getAtualizadaEm());
        assertThat(propostas.findById(criada.id()).orElseThrow().getTitulo()).isEqualTo("Horta atualizada");
        service.curtir(criada.id());
        service.arquivar(criada.id());
        assertThat(service.listar(null, null, null, null, null, 0).totalElementos()).isZero();
        assertThat(propostas.findById(criada.id()).orElseThrow().getStatus()).isEqualTo(StatusProposta.ARQUIVADA);
        assertThat(curtidas.countByPropostaId(criada.id())).isEqualTo(1);
        assertThatThrownBy(() -> service.buscarPorId(criada.id())).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void curtidasConcorrentesSaoIdempotentes() throws Exception {
        var proposta = service.criar(request("Curtidas concorrentes"));
        var executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Object>> tarefas = IntStream.range(0, 20)
                    .mapToObj(i -> (Callable<Object>) () -> service.curtir(proposta.id())).toList();
            for (var futuro : executor.invokeAll(tarefas)) futuro.get();
        } finally {
            executor.shutdownNow();
        }
        assertThat(curtidas.countByPropostaId(proposta.id())).isEqualTo(1);
        assertThat(service.buscarPorId(proposta.id()).curtidaPeloUsuarioAtual()).isTrue();
        assertThatThrownBy(() -> curtidas.insert(new Curtida(PropostaService.USUARIO_DEMONSTRACAO_ID, proposta.id())))
                .isInstanceOf(DuplicateKeyException.class);
        service.removerCurtida(proposta.id());
        service.removerCurtida(proposta.id());
        assertThat(curtidas.countByPropostaId(proposta.id())).isZero();
        assertThat(propostas.existsById(proposta.id())).isTrue();
    }

    @Test
    void filtrosPaginacaoEBuscaLiteral() {
        for (int i = 0; i < 12; i++) service.criar(request("Horta comunitária " + i));
        service.criar(request("Projeto [especial]"));
        var pagina = service.listar("HORTA", CATEGORIA, PUBLICO, CURSO, StatusProposta.PUBLICADA, 0);
        assertThat(pagina.totalElementos()).isEqualTo(12);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        assertThat(pagina.conteudo()).hasSize(10);
        var segunda = service.listar("horta", CATEGORIA, PUBLICO, CURSO, null, 1);
        assertThat(segunda.conteudo()).hasSize(2);
        assertThat(segunda.conteudo()).doesNotContainAnyElementsOf(pagina.conteudo());
        assertThat(service.listar("[especial]", null, null, null, null, 0).totalElementos()).isEqualTo(1);
        assertThat(service.listar(".*", null, null, null, null, 0).totalElementos()).isZero();
        assertThat(service.listar(null, null, null, null, StatusProposta.ARQUIVADA, 0).totalElementos()).isZero();
        assertThat(service.listar(null, null, null, null, null, 20).conteudo()).isEmpty();
    }

    @Test
    void indicesUnicosESiglasOpcionais() {
        Usuario usuario = new Usuario();
        usuario.setEmail("demo@fatec.local");
        assertThatThrownBy(() -> usuarios.insert(usuario)).isInstanceOf(DuplicateKeyException.class);
        Categoria categoria = new Categoria();
        categoria.setNome(categorias.findById(CATEGORIA).orElseThrow().getNome());
        assertThatThrownBy(() -> categorias.insert(categoria)).isInstanceOf(DuplicateKeyException.class);
        for (int i = 0; i < 2; i++) {
            Curso curso = new Curso();
            curso.setNome("Curso sem sigla " + i);
            cursos.insert(curso);
        }
        Curso repetido = new Curso();
        repetido.setNome("Outro curso");
        repetido.setSigla("DSM");
        assertThatThrownBy(() -> cursos.insert(repetido)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void seedNaoSobrescreveAlteracoes() {
        Categoria categoria = categorias.findById(CATEGORIA).orElseThrow();
        categoria.setNome("Nome editado");
        categoria.setAtiva(false);
        categorias.save(categoria);
        dados.run(null);
        assertThat(categorias.count()).isEqualTo(4);
        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(categorias.findById(CATEGORIA).orElseThrow().getNome()).isEqualTo("Nome editado");
        assertThat(categorias.findById(CATEGORIA).orElseThrow().isAtiva()).isFalse();
    }

    @Test
    void referenciasInvalidasOuInativasNaoCriamPropostas() {
        Categoria categoria = categorias.findById(CATEGORIA).orElseThrow();
        categoria.setAtiva(false);
        categorias.save(categoria);
        assertThatThrownBy(() -> service.criar(request("Categoria inativa"))).isInstanceOf(RequisicaoInvalidaException.class);
        categoria.setAtiva(true);
        categorias.save(categoria);
        Curso curso = cursos.findById(CURSO).orElseThrow();
        curso.setAtivo(false);
        cursos.save(curso);
        assertThatThrownBy(() -> service.criar(request("Curso inativo"))).isInstanceOf(RequisicaoInvalidaException.class);
        curso.setAtivo(true);
        cursos.save(curso);
        publicos.deleteById(PUBLICO);
        assertThatThrownBy(() -> service.criar(request("Público ausente"))).isInstanceOf(RequisicaoInvalidaException.class);
        assertThat(propostas.count()).isZero();
    }

    @Test
    void contratoHttpEValidacao() throws Exception {
        mvc.perform(get("/api/propostas/invalido")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/propostas/ffffffffffffffffffffffff")).andExpect(status().isNotFound());
        mvc.perform(get("/api/propostas").param("cursoId", "1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/propostas").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/propostas").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        String corpo = """
                {"titulo":"Horta comunitária", "resumo":"Resumo", "descricao":"Descrição",
                 "categoriaId":"%s", "cursoIds":["%s"], "publicoAlvoIds":["%s"]}
                """.formatted(CATEGORIA, CURSO, PUBLICO);
        mvc.perform(post("/api/propostas").contentType("application/json").content(corpo))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString()).andExpect(jsonPath("$.categoriaId").value(CATEGORIA))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        String id = propostas.findAll().get(0).getId();
        mvc.perform(put("/api/propostas/" + id).contentType("application/json").content(corpo))
                .andExpect(status().isOk());
        mvc.perform(put("/api/propostas/" + id + "/curtida")).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeCurtidas").value(1));
        mvc.perform(delete("/api/propostas/" + id + "/curtida")).andExpect(status().isOk());
        mvc.perform(delete("/api/propostas/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/propostas/" + id)).andExpect(status().isNotFound());
    }
}
