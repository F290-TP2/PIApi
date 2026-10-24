package br.com.fatecararas.piapi.infrastructure.web.rest.controller;

import br.com.fatecararas.piapi.application.dto.AtualizarPropostaRequest;
import br.com.fatecararas.piapi.application.dto.CriarPropostaRequest;
import br.com.fatecararas.piapi.application.dto.PaginaResponse;
import br.com.fatecararas.piapi.application.dto.PropostaResponse;
import br.com.fatecararas.piapi.application.service.PropostaService;
import br.com.fatecararas.piapi.domain.model.StatusProposta;
import br.com.fatecararas.piapi.infrastructure.web.rest.exception.ApiExceptionHandler.ErroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Propostas", description = "Publicação, consulta, atualização, arquivamento e curtidas.")
@RequestMapping("/api/propostas")
public class PropostasResource {

    private final PropostaService propostaService;

    // @RequestParam lê filtros da URL; page começa em zero quando não informado.
    @Operation(operationId = "listarPropostas", summary = "Listar propostas publicadas",
            description = "Até 10 itens por página, ordenados por criação decrescente. Filtros combinados com AND; "
                    + "q busca título, resumo ou descrição. status=ARQUIVADA retorna uma página vazia.")
    @ApiResponse(responseCode = "200", description = "Página encontrada, inclusive quando vazia.")
    @ApiResponse(responseCode = "400", description = "Entrada inválida. Erros tratados usam ErroResponse; "
            + "falhas de conversão em query/path usam o corpo padrão do Spring, sem formato garantido.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    @GetMapping
    public ResponseEntity<PaginaResponse<PropostaResponse>> listar(
            @Parameter(description = "Texto procurado em título, resumo ou descrição.", example = "horta")
            @RequestParam(required = false) String q,
            @Parameter(description = "ID da categoria usada como filtro.", example = "1")
            @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "ID do público-alvo usado como filtro.", example = "1")
            @RequestParam(required = false) Long publicoAlvoId,
            @Parameter(description = "ID do curso usado como filtro.", example = "1")
            @RequestParam(required = false) Long cursoId,
            @Parameter(description = "A consulta é pública: ARQUIVADA produz página vazia.", example = "PUBLICADA")
            @RequestParam(required = false) StatusProposta status,
            @Parameter(description = "Página começando em zero; tamanho fixo de 10.", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(propostaService.listar(q, categoriaId, publicoAlvoId, cursoId, status, page));
    }

    // @PathVariable obtém o identificador que faz parte do caminho.
    @Operation(operationId = "buscarProposta", summary = "Consultar uma proposta publicada")
    @ApiResponse(responseCode = "200", description = "Proposta encontrada.")
    @ApiResponse(responseCode = "404", description = "Proposta não encontrada ou indisponível.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "400", description = "Entrada inválida. Erros tratados usam ErroResponse; "
            + "falhas de conversão em query/path usam o corpo padrão do Spring, sem formato garantido.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    @GetMapping("/{id}")
    public ResponseEntity<PropostaResponse> buscarPorId(@Parameter(description = "ID da proposta.", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(propostaService.buscarPorId(id));
    }

    // @RequestBody converte JSON em DTO; @Valid verifica suas constraints antes do método.
    @Operation(operationId = "criarProposta", summary = "Publicar uma proposta",
            description = "Recebe somente campos editáveis. Autor, status PUBLICADA, ID e datas são definidos pelo servidor.")
    @ApiResponse(responseCode = "201", description = "Proposta criada.",
            headers = @Header(name = "Location", description = "Endereço do detalhe da proposta criada.",
                    schema = @Schema(type = "string", format = "uri", example = "http://localhost:8080/api/propostas/1")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PropostaResponse.class)))
    @ApiResponse(responseCode = "400", description = "DTO inválido, JSON ilegível ou catálogo inexistente/inativo.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class),
                    examples = {
                            @ExampleObject(name = "Validacao", summary = "Exemplo ilustrativo de campo inválido",
                                    value = "{\"instante\":\"2026-10-24T12:00:00Z\",\"status\":400,\"erro\":\"Dados inválidos\","
                                            + "\"mensagem\":\"Revise os campos informados.\",\"campos\":{\"titulo\":\"não deve estar em branco\"}}"),
                            @ExampleObject(name = "Regra", summary = "Categoria inexistente ou inativa",
                                    value = "{\"instante\":\"2026-10-24T12:00:00Z\",\"status\":400,\"erro\":\"Requisição inválida\","
                                            + "\"mensagem\":\"Categoria não existe ou está inativa.\",\"campos\":{}}")
                    }))
    @PostMapping
    public ResponseEntity<PropostaResponse> criar(@Valid @RequestBody CriarPropostaRequest request) {
        PropostaResponse criada = propostaService.criar(request);
        // 201 Created devolve a representação e o endereço do recurso no cabeçalho Location.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    // Atualiza todos os campos editáveis; não é uma atualização parcial (PATCH).
    @Operation(operationId = "atualizarProposta", summary = "Atualizar os campos editáveis",
            description = "PUT exige todos os campos do DTO. Não permite trocar autor, ID, status ou datas.")
    @ApiResponse(responseCode = "200", description = "Proposta atualizada.")
    @ApiResponse(responseCode = "404", description = "Proposta não encontrada ou indisponível.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "400", description = "Entrada inválida. Erros tratados usam ErroResponse; "
            + "falhas de conversão em query/path usam o corpo padrão do Spring, sem formato garantido.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    @PutMapping("/{id}")
    public ResponseEntity<PropostaResponse> atualizar(@Parameter(description = "ID da proposta.", example = "1") @PathVariable Long id,
                                                       @Valid @RequestBody AtualizarPropostaRequest request) {
        return ResponseEntity.ok(propostaService.atualizar(id, request));
    }

    // O service arquiva a proposta; 204 indica sucesso sem corpo na resposta.
    @Operation(operationId = "arquivarProposta", summary = "Arquivar uma proposta",
            description = "Exclusão lógica: preserva a linha e as curtidas. Repetir para um ID existente mantém 204.")
    @ApiResponse(responseCode = "204", description = "Proposta arquivada; sem corpo.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Não existe proposta com esse ID.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "400", description = "Entrada inválida. Erros tratados usam ErroResponse; "
            + "falhas de conversão em query/path usam o corpo padrão do Spring, sem formato garantido.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> arquivar(@Parameter(description = "ID da proposta.", example = "1") @PathVariable Long id) {
        propostaService.arquivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "curtirProposta", summary = "Curtir uma proposta publicada",
            description = "Usa o usuário demonstrativo. Repetições sequenciais não aumentam o contador.")
    @ApiResponse(responseCode = "200", description = "Proposta com a situação da curtida atualizada.")
    @ApiResponse(responseCode = "404", description = "Proposta não encontrada ou indisponível.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "400", description = "Entrada inválida. Erros tratados usam ErroResponse; "
            + "falhas de conversão em query/path usam o corpo padrão do Spring, sem formato garantido.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    @PutMapping("/{id}/curtida")
    public ResponseEntity<PropostaResponse> curtir(@Parameter(description = "ID da proposta.", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(propostaService.curtir(id));
    }

    @Operation(operationId = "removerCurtida", summary = "Remover a curtida",
            description = "Remove a curtida do usuário demonstrativo; devolve a proposta, mesmo se não havia curtida.")
    @ApiResponse(responseCode = "200", description = "Proposta com a situação da curtida atualizada.")
    @ApiResponse(responseCode = "404", description = "Proposta não encontrada ou indisponível.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "400", description = "Entrada inválida. Erros tratados usam ErroResponse; "
            + "falhas de conversão em query/path usam o corpo padrão do Spring, sem formato garantido.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object")))
    @DeleteMapping("/{id}/curtida")
    public ResponseEntity<PropostaResponse> removerCurtida(@Parameter(description = "ID da proposta.", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(propostaService.removerCurtida(id));
    }
}
