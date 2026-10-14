package br.com.fatecararas.piapi;

import br.com.fatecararas.piapi.domain.entity.*;
import br.com.fatecararas.piapi.domain.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class PersistenciaJpaTests {

    @Autowired UsuarioRepository usuarios;
    @Autowired CategoriaRepository categorias;
    @Autowired CursoRepository cursos;
    @Autowired PublicoAlvoRepository publicos;
    @Autowired PropostaRepository propostas;
    @Autowired CurtidaRepository curtidas;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    private Proposta novaProposta() {
        Proposta proposta = new Proposta();
        proposta.setTitulo("Horta comunitária conectada");
        proposta.setResumo("Monitoramento de uma horta local.");
        proposta.setDescricao("Integração de tecnologia e sustentabilidade.");
        proposta.setAutor(usuarios.findById(1L).orElseThrow());
        proposta.setCategoria(categorias.findById(1L).orElseThrow());
        proposta.getCursos().add(cursos.findById(1L).orElseThrow());
        proposta.getCursos().add(cursos.findById(2L).orElseThrow());
        proposta.getPublicosAlvo().add(publicos.findById(1L).orElseThrow());
        return propostas.saveAndFlush(proposta);
    }

    @Test
    void recuperaRelacionamentosDoBanco() {
        Proposta proposta = novaProposta();
        Long propostaId = proposta.getId();
        CurtidaId chave = new CurtidaId(1L, propostaId);
        curtidas.saveAndFlush(new Curtida(proposta.getAutor(), proposta));

        // Esvazia o contexto para verificar uma nova leitura do banco.
        entityManager.clear();

        Proposta lida = propostas.findById(propostaId).orElseThrow();
        assertThat(lida.getAutor().getEmail()).isEqualTo("demo@fatec.local");
        assertThat(lida.getCategoria().getId()).isEqualTo(1L);
        assertThat(lida.getCursos()).extracting(Curso::getId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(lida.getPublicosAlvo()).extracting(PublicoAlvo::getId)
                .containsExactly(1L);
        assertThat(lida.getCriadaEm()).isNotNull();
        assertThat(lida.getAtualizadaEm()).isNotNull();
        Curtida curtida = curtidas.findById(chave).orElseThrow();
        assertThat(curtida.getUsuario().getId()).isEqualTo(1L);
        assertThat(curtida.getProposta().getId()).isEqualTo(propostaId);
        assertThat(curtida.getCriadaEm()).isNotNull();
        assertThat(curtidas.countByIdPropostaId(propostaId)).isEqualTo(1L);

        curtidas.deleteById(chave);
        curtidas.flush();
        entityManager.clear();
        assertThat(curtidas.existsById(chave)).isFalse();
        assertThat(propostas.existsById(propostaId)).isTrue();
        assertThat(usuarios.existsById(1L)).isTrue();
    }

    @Test
    void bancoRejeitaDuasLinhasComAMesmaChaveDeCurtida() {
        Proposta proposta = novaProposta();
        curtidas.saveAndFlush(new Curtida(proposta.getAutor(), proposta));

        // INSERT direto força uma segunda linha, sem a semântica de merge do save.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO curtida (usuario_id, proposta_id, criada_em) VALUES (?, ?, CURRENT_TIMESTAMP)",
                1L, proposta.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
