package br.com.fatecararas.piapi.config;

import br.com.fatecararas.piapi.domain.PropostaService;
import br.com.fatecararas.piapi.domain.entity.*;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Dados didáticos, não migrations: setOnInsert preserva alterações em reinicializações. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dados-demonstrativos", havingValue = "true", matchIfMissing = true)
public class DadosIniciais implements ApplicationRunner {
    private final MongoTemplate mongo;

    @Override
    public void run(ApplicationArguments args) {
        Usuario usuario = new Usuario();
        usuario.setId(PropostaService.USUARIO_DEMONSTRACAO_ID);
        usuario.setNome("Usuário de demonstração");
        usuario.setEmail("demo@fatec.local");
        usuario.setSenhaHash("PENDENTE-ETAPA-SEGURANCA");
        usuario.setCriadoEm(Instant.now());
        inserirSeAusente(usuario);

        String[][] categorias = {
                {"Tecnologia e inovação", "Soluções digitais e novos produtos."},
                {"Sustentabilidade", "Soluções ambientais e uso responsável de recursos."},
                {"Empreendedorismo", "Soluções para negócios e desenvolvimento local."},
                {"Inclusão e cidadania", "Soluções para participação social e acessibilidade."}};
        for (int i = 0; i < categorias.length; i++) {
            Categoria categoria = new Categoria();
            categoria.setId(id(1, i + 1));
            categoria.setNome(categorias[i][0]);
            categoria.setDescricao(categorias[i][1]);
            inserirSeAusente(categoria);
        }
        String[][] cursos = {{"Desenvolvimento de Software Multiplataforma", "DSM"},
                {"Gestão Empresarial", "GE"}, {"Sistemas para Internet", "SI"}};
        for (int i = 0; i < cursos.length; i++) {
            Curso curso = new Curso();
            curso.setId(id(2, i + 1));
            curso.setNome(cursos[i][0]);
            curso.setSigla(cursos[i][1]);
            inserirSeAusente(curso);
        }
        String[][] publicos = {
                {"Comunidade local", "Moradores e organizações da região."},
                {"Pequenos produtores", "Pequenos produtores e cooperativas locais."},
                {"Pequenos negócios", "Microempreendedores e empresas de pequeno porte."},
                {"Estudantes", "Estudantes de escolas e instituições de ensino."}};
        for (int i = 0; i < publicos.length; i++) {
            PublicoAlvo publico = new PublicoAlvo();
            publico.setId(id(3, i + 1));
            publico.setNome(publicos[i][0]);
            publico.setDescricao(publicos[i][1]);
            inserirSeAusente(publico);
        }
    }

    private static String id(int grupo, int numero) {
        return String.format("%012x%012x", grupo, numero);
    }

    private void inserirSeAusente(Object entidade) {
        Document documento = new Document();
        mongo.getConverter().write(entidade, documento);
        Object id = documento.remove("_id");
        Update insercao = new Update();
        documento.forEach(insercao::setOnInsert);
        // Uma operação atômica por documento, sem sobrescrever nome, status ou senha.
        mongo.upsert(Query.query(Criteria.where("_id").is(id)), insercao, entidade.getClass());
    }
}
