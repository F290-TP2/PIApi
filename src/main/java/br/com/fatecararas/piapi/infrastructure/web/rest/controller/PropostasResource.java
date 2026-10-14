package br.com.fatecararas.piapi.infrastructure.web.rest.controller;

import br.com.fatecararas.piapi.application.dto.AtualizarPropostaRequest;
import br.com.fatecararas.piapi.application.dto.CriarPropostaRequest;
import br.com.fatecararas.piapi.application.dto.PaginaResponse;
import br.com.fatecararas.piapi.application.dto.PropostaResponse;
import br.com.fatecararas.piapi.application.service.PropostaService;
import br.com.fatecararas.piapi.domain.model.StatusProposta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

// Lombok gera o construtor para injetar o service declarado como final.
@RequiredArgsConstructor
// Resource é o nome adotado para o controller REST: recebe HTTP e delega ao service.
@RestController
// Prefixo compartilhado pelas rotas desta classe.
@RequestMapping("/api/propostas")
public class PropostasResource {

    private final PropostaService propostaService;

    // @RequestParam lê filtros da URL; page começa em zero quando não informado.
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

    // @PathVariable obtém o identificador que faz parte do caminho.
    @GetMapping("/{id}")
    public ResponseEntity<PropostaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(propostaService.buscarPorId(id));
    }

    // @RequestBody converte JSON em DTO; @Valid verifica suas constraints antes do método.
    @PostMapping
    public ResponseEntity<PropostaResponse> criar(@Valid @RequestBody CriarPropostaRequest request) {
        PropostaResponse criada = propostaService.criar(request);
        // 201 Created devolve a representação e o endereço do recurso no cabeçalho Location.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    // Atualiza todos os campos editáveis; não é uma atualização parcial (PATCH).
    @PutMapping("/{id}")
    public ResponseEntity<PropostaResponse> atualizar(@PathVariable Long id,
                                                       @Valid @RequestBody AtualizarPropostaRequest request) {
        return ResponseEntity.ok(propostaService.atualizar(id, request));
    }

    // O service arquiva a proposta; 204 indica sucesso sem corpo na resposta.
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
