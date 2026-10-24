package br.com.fatecararas.piapi.infrastructure.web.rest.controller;

import br.com.fatecararas.piapi.application.dto.ItemCatalogoResponse;
import br.com.fatecararas.piapi.application.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Resource é o nome adotado para o controller REST: recebe HTTP e delega ao service.
@RestController
// Prefixo compartilhado pelas rotas desta classe.
@Tag(name = "Catálogos", description = "Itens ativos para preencher formulários e filtros.")
@RequestMapping("/api")
public class CatalogosResource {

    private final CatalogoService catalogoService;

    // Com um único construtor, o Spring injeta a dependência sem precisar de @Autowired.
    public CatalogosResource(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @Operation(operationId = "listarCategorias", summary = "Listar categorias ativas", description = "Itens ordenados pelo nome; use os IDs no corpo da proposta.")
    @ApiResponse(responseCode = "200", description = "Lista de itens ativos, possivelmente vazia.")
    @GetMapping("/categorias")
    public ResponseEntity<List<ItemCatalogoResponse>> listarCategorias() {
        return ResponseEntity.ok(catalogoService.listarCategorias());
    }

    @Operation(operationId = "listarCursos", summary = "Listar cursos ativos", description = "Itens ordenados pelo nome; use os IDs no corpo da proposta.")
    @ApiResponse(responseCode = "200", description = "Lista de itens ativos, possivelmente vazia.")
    @GetMapping("/cursos")
    public ResponseEntity<List<ItemCatalogoResponse>> listarCursosAtivos() {
        return ResponseEntity.ok(catalogoService.listarCursos());
    }

    @Operation(operationId = "listarPublicosAlvo", summary = "Listar públicos-alvo ativos", description = "Itens ordenados pelo nome; use os IDs no corpo da proposta.")
    @ApiResponse(responseCode = "200", description = "Lista de itens ativos, possivelmente vazia.")
    @GetMapping("/publicos-alvo")
    public ResponseEntity<List<ItemCatalogoResponse>> listarPublicosAlvoAtivos() {
        return ResponseEntity.ok(catalogoService.listarPublicosAlvo());
    }
}
