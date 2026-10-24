package br.com.fatecararas.piapi.infrastructure.web.rest.exception;

import br.com.fatecararas.piapi.domain.exception.RecursoNaoEncontradoException;
import br.com.fatecararas.piapi.domain.exception.RequisicaoInvalidaException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

// Centraliza a tradução de exceções em respostas HTTP para os controllers.
@RestControllerAdvice
public class ApiExceptionHandler {

    // O service informa a ausência; a camada web escolhe o status HTTP 404.
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(Instant.now(), 404, "Não encontrado", exception.getMessage(), Map.of()));
    }

    // Regras da aplicação rejeitadas, como catálogo inexistente ou inativo.
    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarRequisicaoInvalida(RequisicaoInvalidaException exception) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(Instant.now(), 400, "Requisição inválida", exception.getMessage(), Map.of()));
    }

    // Falha das constraints do DTO: Spring reúne os erros no BindingResult.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException exception) {
        Map<String, String> campos = new LinkedHashMap<>();
        // Guarda uma mensagem por campo; a ordem das violações não é garantida.
        for (FieldError erro : exception.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ErroResponse(Instant.now(), 400,
                "Dados inválidos", "Revise os campos informados.", campos));
    }

    // Corpo JSON ilegível ou incompatível com o DTO; não cobre conversão de query/path.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErroResponse(Instant.now(), 400,
                "Requisição inválida", "O corpo ou um dos parâmetros não tem o formato esperado.", Map.of()));
    }

    @Schema(description = "Erro tratado pelo advice. Falhas de conversão em query/path não seguem este modelo.")
    public record ErroResponse(
            @Schema(description = "Instante do erro em UTC.") Instant instante,
            @Schema(description = "Código HTTP.", example = "400") int status,
            @Schema(description = "Categoria do erro.", example = "Dados inválidos") String erro,
            @Schema(description = "Orientação para o consumidor.", example = "Revise os campos informados.") String mensagem,
            @Schema(description = "Uma mensagem por campo inválido; vazio para erros de regra ou JSON.")
            Map<String, String> campos) {
    }
}
