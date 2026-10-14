package br.com.fatecararas.piapi.application.service;

import br.com.fatecararas.piapi.application.dto.ItemCatalogoResponse;
import br.com.fatecararas.piapi.domain.entity.Categoria;
import br.com.fatecararas.piapi.domain.entity.Curso;
import br.com.fatecararas.piapi.domain.entity.PublicoAlvo;
import br.com.fatecararas.piapi.domain.repository.CategoriaRepository;
import br.com.fatecararas.piapi.domain.repository.CursoRepository;
import br.com.fatecararas.piapi.domain.repository.PublicoAlvoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import br.com.fatecararas.piapi.domain.exception.RequisicaoInvalidaException;

// Service Layer: coordena casos de uso e regras, sem montar respostas HTTP.
@Service
// Injeta os repositories e demais serviços pelos campos final.
@RequiredArgsConstructor
public class CatalogoService {

    private final CategoriaRepository categorias;
    private final CursoRepository cursos;
    private final PublicoAlvoRepository publicos;

    @Transactional(readOnly = true)
    public List<ItemCatalogoResponse> listarCategorias() {
        return categorias.findByAtivaTrueOrderByNomeAsc().stream()
                .map(c -> new ItemCatalogoResponse(c.getId(), c.getNome(), c.getDescricao(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemCatalogoResponse> listarCursos() {
        return cursos.findByAtivoTrueOrderByNomeAsc().stream()
                .map(c -> new ItemCatalogoResponse(c.getId(), c.getNome(), null, c.getSigla()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemCatalogoResponse> listarPublicosAlvo() {
        return publicos.findByAtivoTrueOrderByNomeAsc().stream()
                .map(p -> new ItemCatalogoResponse(p.getId(), p.getNome(), p.getDescricao(), null))
                .toList();
    }

    // @NotNull no DTO não verifica existência nem situação do registro; fazemos isso aqui.
    @Transactional(readOnly = true)
    public Categoria categoriaAtiva(Long id) {
        return categorias.findByIdAndAtivaTrue(id)
                .orElseThrow(() -> new RequisicaoInvalidaException("Categoria não existe ou está inativa."));
    }

    @Transactional(readOnly = true)
    public Set<Curso> cursosAtivos(List<Long> ids) {
        List<Curso> encontrados = cursos.findAllById(ids);
        // IDs repetidos representam uma única associação no conjunto da proposta.
        long esperados = ids.stream().distinct().count();
        if (encontrados.size() != esperados || encontrados.stream().anyMatch(c -> !c.isAtivo())) {
            throw new RequisicaoInvalidaException("Um ou mais identificadores de curso não existem ou estão inativos.");
        }
        return new HashSet<>(encontrados);
    }

    @Transactional(readOnly = true)
    public Set<PublicoAlvo> publicosAlvoAtivos(List<Long> ids) {
        List<PublicoAlvo> encontrados = publicos.findAllById(ids);
        // IDs repetidos representam uma única associação no conjunto da proposta.
        long esperados = ids.stream().distinct().count();
        if (encontrados.size() != esperados || encontrados.stream().anyMatch(p -> !p.isAtivo())) {
            throw new RequisicaoInvalidaException("Um ou mais identificadores de público-alvo não existem ou estão inativos.");
        }
        return new HashSet<>(encontrados);
    }

    @Transactional(readOnly = true)
    public Curso cursoAtivo(Long id) {
        return cursos.findById(id)
                .filter(Curso::isAtivo)
                .orElseThrow(() -> new RequisicaoInvalidaException("Curso não existe ou está inativo."));
    }

    @Transactional(readOnly = true)
    public PublicoAlvo publicoAlvoAtivo(Long id) {
        return publicos.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RequisicaoInvalidaException("Público-alvo não existe ou está inativo."));
    }
}
