package br.com.fatecararas.piapi.resources;

import br.com.fatecararas.piapi.api.dto.AtualizarPropostaRequest;
import br.com.fatecararas.piapi.api.dto.CriarPropostaRequest;
import br.com.fatecararas.piapi.api.dto.PaginaResponse;
import br.com.fatecararas.piapi.api.dto.PropostaResponse;
import br.com.fatecararas.piapi.domain.PropostaService;
import br.com.fatecararas.piapi.domain.StatusProposta;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/propostas")
public class PropostasResource {

    private final PropostaService propostaService;

    public PropostasResource(PropostaService propostaService) {
        this.propostaService = propostaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<PropostaResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long publicoAlvoId,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) StatusProposta status,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(propostaService.listar(q, categoriaId, publicoAlvoId, cursoId, status, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropostaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(propostaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PropostaResponse> criar(@Valid @RequestBody CriarPropostaRequest request) {
        PropostaResponse criada = propostaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropostaResponse> atualizar(@PathVariable Long id,
                                                       @Valid @RequestBody AtualizarPropostaRequest request) {
        return ResponseEntity.ok(propostaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> arquivar(@PathVariable Long id) {
        propostaService.arquivar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/curtida")
    public ResponseEntity<PropostaResponse> curtir(@PathVariable Long id) {
        return ResponseEntity.ok(propostaService.curtir(id));
    }

    @DeleteMapping("/{id}/curtida")
    public ResponseEntity<PropostaResponse> removerCurtida(@PathVariable Long id) {
        return ResponseEntity.ok(propostaService.removerCurtida(id));
    }
}
