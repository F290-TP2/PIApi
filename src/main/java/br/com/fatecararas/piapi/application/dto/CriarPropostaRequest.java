package br.com.fatecararas.piapi.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// DTO de entrada: recebe apenas os campos que o cliente pode informar.
// As constraints são de Jakarta Validation e são verificadas pelo MVC com @Valid.
public record CriarPropostaRequest(
        @NotBlank @Size(min = 5, max = 120) String titulo,
        @NotBlank @Size(max = 300) String resumo,
        @NotBlank @Size(max = 10_000) String descricao,
        // A lista precisa ter itens; cada ID deve ser não nulo. Existência é regra do service.
        @NotEmpty List<@NotNull Long> publicoAlvoIds,
        @NotNull Long categoriaId,
        @NotEmpty List<@NotNull Long> cursoIds) {
}
