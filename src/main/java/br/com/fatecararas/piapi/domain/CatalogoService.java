package br.com.fatecararas.piapi.domain;

import br.com.fatecararas.piapi.api.dto.ItemCatalogoResponse;
import br.com.fatecararas.piapi.domain.entity.Categoria;
import br.com.fatecararas.piapi.domain.entity.Curso;
import br.com.fatecararas.piapi.domain.entity.PublicoAlvo;
import br.com.fatecararas.piapi.domain.repository.CategoriaRepository;
import br.com.fatecararas.piapi.domain.repository.CursoRepository;
import br.com.fatecararas.piapi.domain.repository.PublicoAlvoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final CategoriaRepository categorias;
    private final CursoRepository cursos;
    private final PublicoAlvoRepository publicos;

    public List<ItemCatalogoResponse> listarCategorias() {
        return categorias.findByAtivaTrueOrderByNomeAsc().stream()
                .map(c -> new ItemCatalogoResponse(c.getId(), c.getNome(), c.getDescricao(), null))
                .toList();
    }

    public List<ItemCatalogoResponse> listarCursos() {
        return cursos.findByAtivoTrueOrderByNomeAsc().stream()
                .map(c -> new ItemCatalogoResponse(c.getId(), c.getNome(), null, c.getSigla()))
                .toList();
    }

    public List<ItemCatalogoResponse> listarPublicosAlvo() {
        return publicos.findByAtivoTrueOrderByNomeAsc().stream()
                .map(p -> new ItemCatalogoResponse(p.getId(), p.getNome(), p.getDescricao(), null))
                .toList();
    }

    public Categoria categoriaAtiva(String id) {
        return categorias.findByIdAndAtivaTrue(Ids.normalizar(id))
                .orElseThrow(() -> new RequisicaoInvalidaException("Categoria não existe ou está inativa."));
    }

    public Set<Curso> cursosAtivos(List<String> ids) {
        ids = ids.stream().map(Ids::normalizar).toList();
        List<Curso> encontrados = cursos.findAllById(ids);
        long esperados = ids.stream().distinct().count();
        if (encontrados.size() != esperados || encontrados.stream().anyMatch(c -> !c.isAtivo())) {
            throw new RequisicaoInvalidaException("Um ou mais identificadores de curso não existem ou estão inativos.");
        }
        return new HashSet<>(encontrados);
    }

    public Set<PublicoAlvo> publicosAlvoAtivos(List<String> ids) {
        ids = ids.stream().map(Ids::normalizar).toList();
        List<PublicoAlvo> encontrados = publicos.findAllById(ids);
        long esperados = ids.stream().distinct().count();
        if (encontrados.size() != esperados || encontrados.stream().anyMatch(p -> !p.isAtivo())) {
            throw new RequisicaoInvalidaException("Um ou mais identificadores de público-alvo não existem ou estão inativos.");
        }
        return new HashSet<>(encontrados);
    }

    public Curso cursoAtivo(String id) {
        return cursos.findById(Ids.normalizar(id))
                .filter(Curso::isAtivo)
                .orElseThrow(() -> new RequisicaoInvalidaException("Curso não existe ou está inativo."));
    }

    public PublicoAlvo publicoAlvoAtivo(String id) {
        return publicos.findByIdAndAtivoTrue(Ids.normalizar(id))
                .orElseThrow(() -> new RequisicaoInvalidaException("Público-alvo não existe ou está inativo."));
    }
}
