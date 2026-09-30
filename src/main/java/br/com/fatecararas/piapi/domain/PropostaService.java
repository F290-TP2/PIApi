package br.com.fatecararas.piapi.domain;

import br.com.fatecararas.piapi.api.dto.AtualizarPropostaRequest;
import br.com.fatecararas.piapi.api.dto.CriarPropostaRequest;
import br.com.fatecararas.piapi.api.dto.PaginaResponse;
import br.com.fatecararas.piapi.api.dto.PropostaResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PropostaService {

    public static final int TAMANHO_PAGINA = 10;
    private static final long USUARIO_DEMONSTRACAO_ID = 1L;
    private static final String USUARIO_DEMONSTRACAO_NOME = "Usuário de demonstração";

    private final CatalogoService catalogoService;
    private final AtomicLong sequencia = new AtomicLong(1);
    private final Map<Long, Proposta> propostas = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> curtidasPorProposta = new ConcurrentHashMap<>();

    public PropostaService(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
        Instant agora = Instant.now();
        propostas.put(1L, new Proposta(
                1L,
                "Horta inteligente para a comunidade",
                "Sistema acessível para monitorar uma horta comunitária.",
                "Proposta interdisciplinar para monitorar a umidade do solo e apoiar o cultivo comunitário.",
                List.of(1L, 2L),
                2L,
                List.of(1L, 2L),
                USUARIO_DEMONSTRACAO_ID,
                USUARIO_DEMONSTRACAO_NOME,
                StatusProposta.PUBLICADA,
                agora,
                agora));
    }

    public PaginaResponse<PropostaResponse> listar(String q, Long categoriaId, Long publicoAlvoId,
                                                    Long cursoId, StatusProposta status, int page) {
        if (page < 0) {
            throw new RequisicaoInvalidaException("O parâmetro page deve ser maior ou igual a zero.");
        }
        List<Proposta> resultado = propostas.values().stream()
                .filter(proposta -> proposta.status() == StatusProposta.PUBLICADA)
                .filter(proposta -> status == null || proposta.status() == status)
                .filter(proposta -> q == null || q.isBlank() || correspondeTexto(proposta, q))
                .filter(proposta -> categoriaId == null || proposta.categoriaId().equals(categoriaId))
                .filter(proposta -> publicoAlvoId == null || proposta.publicoAlvoIds().contains(publicoAlvoId))
                .filter(proposta -> cursoId == null || proposta.cursoIds().contains(cursoId))
                .sorted(Comparator.comparing(Proposta::criadaEm).reversed())
                .toList();

        long inicioLong = (long) page * TAMANHO_PAGINA;
        int inicio = (int) Math.min(inicioLong, resultado.size());
        int fim = Math.min(inicio + TAMANHO_PAGINA, resultado.size());
        List<PropostaResponse> conteudo = resultado.subList(inicio, fim).stream().map(this::paraResponse).toList();
        int totalPaginas = (int) Math.ceil((double) resultado.size() / TAMANHO_PAGINA);
        return new PaginaResponse<>(conteudo, page, TAMANHO_PAGINA, resultado.size(), totalPaginas);
    }

    public PropostaResponse buscarPorId(Long id) {
        Proposta proposta = obterProposta(id);
        if (proposta.status() != StatusProposta.PUBLICADA) {
            throw new RecursoNaoEncontradoException("Proposta " + id + " não encontrada.");
        }
        return paraResponse(proposta);
    }

    public PropostaResponse criar(CriarPropostaRequest request) {
        catalogoService.validarReferencias(request.categoriaId(), request.cursoIds(), request.publicoAlvoIds());
        long id = sequencia.incrementAndGet();
        Instant agora = Instant.now();
        Proposta proposta = new Proposta(id, request.titulo(), request.resumo(), request.descricao(),
                distinct(request.publicoAlvoIds()), request.categoriaId(), distinct(request.cursoIds()),
                USUARIO_DEMONSTRACAO_ID, USUARIO_DEMONSTRACAO_NOME, StatusProposta.PUBLICADA, agora, agora);
        propostas.put(id, proposta);
        return paraResponse(proposta);
    }

    public PropostaResponse atualizar(Long id, AtualizarPropostaRequest request) {
        buscarPorId(id);
        Proposta atual = obterProposta(id);
        catalogoService.validarReferencias(request.categoriaId(), request.cursoIds(), request.publicoAlvoIds());
        Proposta atualizada = new Proposta(id, request.titulo(), request.resumo(), request.descricao(),
                distinct(request.publicoAlvoIds()), request.categoriaId(), distinct(request.cursoIds()),
                atual.autorId(), atual.autorNome(), atual.status(), atual.criadaEm(), Instant.now());
        propostas.put(id, atualizada);
        return paraResponse(atualizada);
    }

    public void arquivar(Long id) {
        Proposta atual = obterProposta(id);
        propostas.put(id, new Proposta(atual.id(), atual.titulo(), atual.resumo(), atual.descricao(),
                atual.publicoAlvoIds(), atual.categoriaId(), atual.cursoIds(), atual.autorId(), atual.autorNome(),
                StatusProposta.ARQUIVADA, atual.criadaEm(), Instant.now()));
    }

    public PropostaResponse curtir(Long id) {
        buscarPorId(id);
        curtidasPorProposta.computeIfAbsent(id, ignorado -> ConcurrentHashMap.newKeySet())
                .add(USUARIO_DEMONSTRACAO_ID);
        return buscarPorId(id);
    }

    public PropostaResponse removerCurtida(Long id) {
        buscarPorId(id);
        curtidasPorProposta.computeIfAbsent(id, ignorado -> ConcurrentHashMap.newKeySet())
                .remove(USUARIO_DEMONSTRACAO_ID);
        return buscarPorId(id);
    }

    private boolean correspondeTexto(Proposta proposta, String termo) {
        String busca = termo.trim().toLowerCase();
        return proposta.titulo().toLowerCase().contains(busca)
                || proposta.resumo().toLowerCase().contains(busca)
                || proposta.descricao().toLowerCase().contains(busca);
    }

    private Proposta obterProposta(Long id) {
        Proposta proposta = propostas.get(id);
        if (proposta == null) {
            throw new RecursoNaoEncontradoException("Proposta " + id + " não encontrada.");
        }
        return proposta;
    }

    private List<Long> distinct(List<Long> ids) {
        return new ArrayList<>(ids.stream().distinct().toList());
    }

    private PropostaResponse paraResponse(Proposta proposta) {
        Set<Long> curtidas = curtidasPorProposta.getOrDefault(proposta.id(), Set.of());
        return new PropostaResponse(proposta.id(), proposta.titulo(), proposta.resumo(), proposta.descricao(),
                proposta.publicoAlvoIds(), proposta.categoriaId(), proposta.cursoIds(), proposta.autorId(),
                proposta.autorNome(), proposta.status(), curtidas.size(),
                curtidas.contains(USUARIO_DEMONSTRACAO_ID), proposta.criadaEm(), proposta.atualizadaEm());
    }
}
