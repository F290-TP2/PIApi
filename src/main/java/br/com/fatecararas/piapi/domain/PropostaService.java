package br.com.fatecararas.piapi.domain;

import br.com.fatecararas.piapi.api.dto.AtualizarPropostaRequest;
import br.com.fatecararas.piapi.api.dto.CriarPropostaRequest;
import br.com.fatecararas.piapi.api.dto.PaginaResponse;
import br.com.fatecararas.piapi.api.dto.PropostaResponse;
import br.com.fatecararas.piapi.domain.entity.Curtida;
import br.com.fatecararas.piapi.domain.entity.Curso;
import br.com.fatecararas.piapi.domain.entity.Proposta;
import br.com.fatecararas.piapi.domain.entity.PublicoAlvo;
import br.com.fatecararas.piapi.domain.entity.Usuario;
import br.com.fatecararas.piapi.domain.repository.CurtidaRepository;
import br.com.fatecararas.piapi.domain.repository.PropostaRepository;
import br.com.fatecararas.piapi.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropostaService {

    public static final int TAMANHO_PAGINA = 10;
    public static final String USUARIO_DEMONSTRACAO_ID = "000000000000000000000001";

    private final PropostaRepository propostas;
    private final UsuarioRepository usuarios;
    private final CurtidaRepository curtidas;
    private final CatalogoService catalogo;
    private final MongoTemplate mongo;

    public PaginaResponse<PropostaResponse> listar(String q, String categoriaId, String publicoAlvoId,
                                                   String cursoId, StatusProposta status, int page) {
        if (page < 0) {
            throw new RequisicaoInvalidaException("O parâmetro page deve ser maior ou igual a zero.");
        }

        List<Criteria> criterios = new ArrayList<>();
        criterios.add(Criteria.where("status").is(StatusProposta.PUBLICADA));
        if (status != null) criterios.add(Criteria.where("status").is(status));
        if (q != null && !q.isBlank()) {
            Pattern termo = Pattern.compile(Pattern.quote(q.trim()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            criterios.add(new Criteria().orOperator(Criteria.where("titulo").regex(termo),
                    Criteria.where("resumo").regex(termo), Criteria.where("descricao").regex(termo)));
        }
        if (categoriaId != null) criterios.add(Criteria.where("categoriaId").is(Ids.normalizar(categoriaId)));
        if (publicoAlvoId != null) criterios.add(Criteria.where("publicoAlvoIds").is(Ids.normalizar(publicoAlvoId)));
        if (cursoId != null) criterios.add(Criteria.where("cursoIds").is(Ids.normalizar(cursoId)));
        Query consulta = new Query(new Criteria().andOperator(criterios));
        long total = mongo.count(consulta, Proposta.class);
        // O ID desempata datas iguais para manter a paginação determinística.
        consulta.with(PageRequest.of(page, TAMANHO_PAGINA,
                Sort.by(Sort.Direction.DESC, "criadaEm", "id")));
        List<PropostaResponse> itens = mongo.find(consulta, Proposta.class).stream()
                .map(this::paraResponse).toList();
        return new PaginaResponse<>(itens, page, TAMANHO_PAGINA, total,
                (int) Math.ceil((double) total / TAMANHO_PAGINA));
    }

    public PropostaResponse buscarPorId(String id) {
        return paraResponse(buscarEntidadePublicada(id));
    }

    public PropostaResponse criar(CriarPropostaRequest request) {
        Proposta proposta = new Proposta();
        proposta.setTitulo(request.titulo());
        proposta.setResumo(request.resumo());
        proposta.setDescricao(request.descricao());
        proposta.setCategoriaId(catalogo.categoriaAtiva(request.categoriaId()).getId());
        proposta.setCursoIds(catalogo.cursosAtivos(request.cursoIds()).stream()
                .map(Curso::getId).collect(Collectors.toSet()));
        proposta.setPublicoAlvoIds(catalogo.publicosAlvoAtivos(request.publicoAlvoIds()).stream()
                .map(PublicoAlvo::getId).collect(Collectors.toSet()));
        proposta.setAutorId(usuarioDemonstracao().getId());
        return paraResponse(propostas.save(proposta));
    }

    public PropostaResponse atualizar(String id, AtualizarPropostaRequest request) {
        Proposta proposta = buscarEntidadePublicada(id);
        proposta.setTitulo(request.titulo());
        proposta.setResumo(request.resumo());
        proposta.setDescricao(request.descricao());
        proposta.setCategoriaId(catalogo.categoriaAtiva(request.categoriaId()).getId());
        proposta.setCursoIds(catalogo.cursosAtivos(request.cursoIds()).stream()
                .map(Curso::getId).collect(Collectors.toSet()));
        proposta.setPublicoAlvoIds(catalogo.publicosAlvoAtivos(request.publicoAlvoIds()).stream()
                .map(PublicoAlvo::getId).collect(Collectors.toSet()));
        return paraResponse(propostas.save(proposta));
    }

    public void arquivar(String id) {
        Proposta proposta = propostas.findById(Ids.normalizar(id))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta " + id + " não encontrada."));
        proposta.setStatus(StatusProposta.ARQUIVADA);
        propostas.save(proposta);
    }

    public PropostaResponse curtir(String id) {
        Proposta proposta = buscarEntidadePublicada(id);
        Usuario usuario = usuarioDemonstracao();
        try {
            curtidas.insert(new Curtida(usuario.getId(), proposta.getId()));
        } catch (DuplicateKeyException repetida) {
            // Apenas a duplicação do par esperado representa uma curtida idempotente.
            if (!curtidas.existsByUsuarioIdAndPropostaId(usuario.getId(), proposta.getId())) throw repetida;
        }
        return paraResponse(proposta);
    }

    public PropostaResponse removerCurtida(String id) {
        Proposta proposta = buscarEntidadePublicada(id);
        Usuario usuario = usuarioDemonstracao();
        curtidas.deleteByUsuarioIdAndPropostaId(usuario.getId(), proposta.getId());
        return paraResponse(proposta);
    }

    private Proposta buscarEntidadePublicada(String id) {
        return propostas.findById(Ids.normalizar(id))
                .filter(p -> p.getStatus() == StatusProposta.PUBLICADA)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta " + id + " não encontrada."));
    }

    private Usuario usuarioDemonstracao() {
        return usuarios.findById(USUARIO_DEMONSTRACAO_ID)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário demonstrativo ausente."));
    }

    private PropostaResponse paraResponse(Proposta proposta) {
        Usuario autor = usuarios.findById(proposta.getAutorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Autor não encontrado."));
        return new PropostaResponse(
                proposta.getId(),
                proposta.getTitulo(),
                proposta.getResumo(),
                proposta.getDescricao(),
                proposta.getPublicoAlvoIds().stream().sorted().toList(),
                proposta.getCategoriaId(),
                proposta.getCursoIds().stream().sorted().toList(),
                proposta.getAutorId(),
                autor.getNome(),
                proposta.getStatus(),
                (int) curtidas.countByPropostaId(proposta.getId()),
                curtidas.existsByUsuarioIdAndPropostaId(USUARIO_DEMONSTRACAO_ID, proposta.getId()),
                proposta.getCriadaEm(),
                proposta.getAtualizadaEm());
    }
}
