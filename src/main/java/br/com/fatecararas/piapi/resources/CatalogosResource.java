package br.com.fatecararas.piapi.resources;

import br.com.fatecararas.piapi.api.dto.ItemCatalogoResponse;
import br.com.fatecararas.piapi.domain.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogosResource {

    private final CatalogoService catalogoService;

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
