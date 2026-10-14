package br.com.fatecararas.piapi.infrastructure.web.rest.exception;

import br.com.fatecararas.piapi.domain.exception.RecursoNaoEncontradoException;
import br.com.fatecararas.piapi.domain.exception.RequisicaoInvalidaException;
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

    public record ErroResponse(Instant instante, int status, String erro, String mensagem,
                               Map<String, String> campos) {
    }
}
