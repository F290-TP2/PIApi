package br.com.fatecararas.piapi.application.service;

import br.com.fatecararas.piapi.application.dto.AtualizarPropostaRequest;
import br.com.fatecararas.piapi.application.dto.CriarPropostaRequest;
import br.com.fatecararas.piapi.application.dto.PaginaResponse;
import br.com.fatecararas.piapi.application.dto.PropostaResponse;
import br.com.fatecararas.piapi.domain.entity.Curtida;
import br.com.fatecararas.piapi.domain.entity.CurtidaId;
import br.com.fatecararas.piapi.domain.entity.Curso;
import br.com.fatecararas.piapi.domain.entity.Proposta;
import br.com.fatecararas.piapi.domain.entity.PublicoAlvo;
import br.com.fatecararas.piapi.domain.entity.Usuario;
import br.com.fatecararas.piapi.domain.repository.CurtidaRepository;
import br.com.fatecararas.piapi.domain.repository.PropostaRepository;
import br.com.fatecararas.piapi.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import br.com.fatecararas.piapi.domain.exception.RequisicaoInvalidaException;
import br.com.fatecararas.piapi.domain.exception.RecursoNaoEncontradoException;
import br.com.fatecararas.piapi.domain.model.StatusProposta;

// Service Layer: coordena casos de uso e regras, sem montar respostas HTTP.
@Service
// Injeta os repositories e demais serviços pelos campos final.
@RequiredArgsConstructor
public class PropostaService {

    public static final int TAMANHO_PAGINA = 10;
    // Identidade temporária carregada pela migration; autenticação entra em outra etapa.
    private static final long USUARIO_DEMONSTRACAO_ID = 1L;

    private final PropostaRepository propostas;
    private final UsuarioRepository usuarios;
    private final CurtidaRepository curtidas;
    private final CatalogoService catalogo;

    // A transação mantém as relações disponíveis ao converter entidades em DTOs.
    @Transactional(readOnly = true)
    public PaginaResponse<PropostaResponse> listar(String q, Long categoriaId, Long publicoAlvoId,
                                                   Long cursoId, StatusProposta status, int page) {
        if (page < 0) {
            throw new RequisicaoInvalidaException("O parâmetro page deve ser maior ou igual a zero.");
        }

        // Os filtros se combinam com AND; a consulta pública sempre exige PUBLICADA.
        Specification<Proposta> filtros = (root, query, cb) -> {
            List<Predicate> criterios = new ArrayList<>();
            criterios.add(cb.equal(root.get("status"), StatusProposta.PUBLICADA));
            if (status != null) {
                criterios.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String termo = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                criterios.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), termo),
                        cb.like(cb.lower(root.get("resumo")), termo),
                        cb.like(cb.lower(root.get("descricao")), termo)));
            }
            if (categoriaId != null) {
                criterios.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (publicoAlvoId != null) {
                Join<Proposta, PublicoAlvo> alvo = root.join("publicosAlvo");
                criterios.add(cb.equal(alvo.get("id"), publicoAlvoId));
            }
            if (cursoId != null) {
                Join<Proposta, Curso> curso = root.join("cursos");
                criterios.add(cb.equal(curso.get("id"), cursoId));
            }
            query.distinct(true);
            return cb.and(criterios.toArray(Predicate[]::new));
        };

        Page<Proposta> pagina = propostas.findAll(filtros,
                PageRequest.of(page, TAMANHO_PAGINA, Sort.by(Sort.Direction.DESC, "criadaEm")));
        List<PropostaResponse> itens = pagina.getContent().stream().map(this::paraResponse).toList();
        return new PaginaResponse<>(itens, pagina.getNumber(), TAMANHO_PAGINA,
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PropostaResponse buscarPorId(Long id) {
        return paraResponse(buscarEntidadePublicada(id));
    }

    // Uma transação engloba as consultas aos catálogos e a gravação da proposta.
    @Transactional
    public PropostaResponse criar(CriarPropostaRequest request) {
        Proposta proposta = new Proposta();
        proposta.setTitulo(request.titulo());
        proposta.setResumo(request.resumo());
        proposta.setDescricao(request.descricao());
        proposta.setCategoria(catalogo.categoriaAtiva(request.categoriaId()));
        proposta.setCursos(catalogo.cursosAtivos(request.cursoIds()));
        proposta.setPublicosAlvo(catalogo.publicosAlvoAtivos(request.publicoAlvoIds()));
        proposta.setAutor(usuarioDemonstracao());
        return paraResponse(propostas.save(proposta));
    }

    @Transactional
    public PropostaResponse atualizar(Long id, AtualizarPropostaRequest request) {
        Proposta proposta = buscarEntidadePublicada(id);
        proposta.setTitulo(request.titulo());
        proposta.setResumo(request.resumo());
        proposta.setDescricao(request.descricao());
        proposta.setCategoria(catalogo.categoriaAtiva(request.categoriaId()));
        proposta.setCursos(catalogo.cursosAtivos(request.cursoIds()));
        proposta.setPublicosAlvo(catalogo.publicosAlvoAtivos(request.publicoAlvoIds()));
        // A entidade já está gerenciada: dirty checking detecta mudanças; flush envia o SQL.
        propostas.flush();
        return paraResponse(proposta);
    }

    @Transactional
    public void arquivar(Long id) {
        Proposta proposta = propostas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta " + id + " não encontrada."));
        // Exclusão lógica: a mudança será persistida ao concluir a transação.
        proposta.setStatus(StatusProposta.ARQUIVADA);
    }

    @Transactional
    public PropostaResponse curtir(Long id) {
        Proposta proposta = buscarEntidadePublicada(id);
        Usuario usuario = usuarioDemonstracao();
        CurtidaId curtidaId = new CurtidaId(usuario.getId(), proposta.getId());
        if (!curtidas.existsById(curtidaId)) {
            curtidas.save(new Curtida(usuario, proposta));
        }
        return paraResponse(proposta);
    }

    @Transactional
    public PropostaResponse removerCurtida(Long id) {
        Proposta proposta = buscarEntidadePublicada(id);
        Usuario usuario = usuarioDemonstracao();
        curtidas.deleteById(new CurtidaId(usuario.getId(), proposta.getId()));
        return paraResponse(proposta);
    }

    private Proposta buscarEntidadePublicada(Long id) {
        return propostas.findById(id)
                .filter(p -> p.getStatus() == StatusProposta.PUBLICADA)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta " + id + " não encontrada."));
    }

    private Usuario usuarioDemonstracao() {
        return usuarios.findById(USUARIO_DEMONSTRACAO_ID)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário demonstrativo ausente."));
    }

    // Executado dentro dos métodos transacionais: lê relações lazy e monta apenas a saída.
    private PropostaResponse paraResponse(Proposta proposta) {
        return new PropostaResponse(
                proposta.getId(),
                proposta.getTitulo(),
                proposta.getResumo(),
                proposta.getDescricao(),
                proposta.getPublicosAlvo().stream().map(PublicoAlvo::getId).toList(),
                proposta.getCategoria().getId(),
                proposta.getCursos().stream().map(Curso::getId).toList(),
                proposta.getAutor().getId(),
                proposta.getAutor().getNome(),
                proposta.getStatus(),
                (int) curtidas.countByIdPropostaId(proposta.getId()),
                curtidas.existsById(new CurtidaId(USUARIO_DEMONSTRACAO_ID, proposta.getId())),
                proposta.getCriadaEm(),
                proposta.getAtualizadaEm());
    }
}
