package br.com.fatecararas.piapi.infrastructure.web.rest.controller;

import br.com.fatecararas.piapi.application.dto.ItemCatalogoResponse;
import br.com.fatecararas.piapi.application.service.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Resource é o nome adotado para o controller REST: recebe HTTP e delega ao service.
@RestController
// Prefixo compartilhado pelas rotas desta classe.
@RequestMapping("/api")
public class CatalogosResource {

    private final CatalogoService catalogoService;

    // Com um único construtor, o Spring injeta a dependência sem precisar de @Autowired.
    public CatalogosResource(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<ItemCatalogoResponse>> listarCategorias() {
        return ResponseEntity.ok(catalogoService.listarCategorias());
    }

    @GetMapping("/cursos")
    public ResponseEntity<List<ItemCatalogoResponse>> listarCursosAtivos() {
        return ResponseEntity.ok(catalogoService.listarCursos());
    }

    @GetMapping("/publicos-alvo")
    public ResponseEntity<List<ItemCatalogoResponse>> listarPublicosAlvoAtivos() {
        return ResponseEntity.ok(catalogoService.listarPublicosAlvo());
    }
}
