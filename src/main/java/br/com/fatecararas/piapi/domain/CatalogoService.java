package br.com.fatecararas.piapi.domain;

import br.com.fatecararas.piapi.api.dto.ItemCatalogoResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CatalogoService {

    private final List<ItemCatalogoResponse> categorias = List.of(
            new ItemCatalogoResponse(1L, "Tecnologia e inovação", "Soluções digitais e novos produtos.", null),
            new ItemCatalogoResponse(2L, "Sustentabilidade", "Soluções ambientais e uso responsável de recursos.", null),
            new ItemCatalogoResponse(3L, "Empreendedorismo", "Soluções para negócios e desenvolvimento local.", null),
            new ItemCatalogoResponse(4L, "Inclusão e cidadania", "Soluções para participação social e acessibilidade.", null));

    private final List<ItemCatalogoResponse> cursos = List.of(
            new ItemCatalogoResponse(1L, "Desenvolvimento de Software Multiplataforma", null, "DSM"),
            new ItemCatalogoResponse(2L, "Gestão Empresarial", null, "GE"),
            new ItemCatalogoResponse(3L, "Sistemas para Internet", null, "SI"));

    private final List<ItemCatalogoResponse> publicosAlvo = List.of(
            new ItemCatalogoResponse(1L, "Comunidade local", "Moradores e organizações da região.", null),
            new ItemCatalogoResponse(2L, "Pequenos produtores", "Pequenos produtores e cooperativas locais.", null),
            new ItemCatalogoResponse(3L, "Pequenos negócios", "Microempreendedores e empresas de pequeno porte.", null),
            new ItemCatalogoResponse(4L, "Estudantes", "Estudantes de escolas e instituições de ensino.", null));

    public List<ItemCatalogoResponse> listarCategorias() {
        return categorias;
    }

    public List<ItemCatalogoResponse> listarCursos() {
        return cursos;
    }

    public List<ItemCatalogoResponse> listarPublicosAlvo() {
        return publicosAlvo;
    }

    public void validarReferencias(Long categoriaId, List<Long> cursoIds, List<Long> publicoAlvoIds) {
        validarExiste(categorias, categoriaId, "Categoria");
        validarTodos(cursos, cursoIds, "Curso");
        validarTodos(publicosAlvo, publicoAlvoIds, "Público-alvo");
    }

    private void validarTodos(List<ItemCatalogoResponse> catalogo, List<Long> ids, String tipo) {
        Set<Long> existentes = catalogo.stream().map(ItemCatalogoResponse::id).collect(java.util.stream.Collectors.toSet());
        if (ids.stream().anyMatch(id -> !existentes.contains(id))) {
            throw new RequisicaoInvalidaException("Um ou mais identificadores de " + tipo.toLowerCase() + " não existem ou estão inativos.");
        }
    }

    private void validarExiste(List<ItemCatalogoResponse> catalogo, Long id, String tipo) {
        if (catalogo.stream().noneMatch(item -> item.id().equals(id))) {
            throw new RequisicaoInvalidaException(tipo + " não existe ou está inativa.");
        }
    }
}
