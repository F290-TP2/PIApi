package br.com.fatecararas.piapi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarPropostaRequest(
        @NotBlank @Size(min = 5, max = 120) String titulo,
        @NotBlank @Size(max = 300) String resumo,
        @NotBlank @Size(max = 10_000) String descricao,
        @NotEmpty List<@NotNull Long> publicoAlvoIds,
        @NotNull Long categoriaId,
        @NotEmpty List<@NotNull Long> cursoIds) {
}
