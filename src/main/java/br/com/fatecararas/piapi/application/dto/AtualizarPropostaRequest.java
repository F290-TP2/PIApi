package br.com.fatecararas.piapi.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// DTO de entrada: recebe apenas os campos que o cliente pode informar.
// As constraints são de Jakarta Validation e são verificadas pelo MVC com @Valid.
@Schema(description = "Campos editáveis da proposta. Todos são obrigatórios; IDs devem existir em catálogos ativos.")
public record AtualizarPropostaRequest(
        @Schema(description = "Título apresentado na listagem.", example = "Horta comunitária conectada")
        @NotBlank @Size(min = 5, max = 120) String titulo,
        @Schema(description = "Resumo para cartões e resultados.", example = "Monitoramento acessível de uma horta local.")
        @NotBlank @Size(max = 300) String resumo,
        @Schema(description = "Descrição detalhada da proposta.", example = "Integração de tecnologia e sustentabilidade na comunidade.")
        @NotBlank @Size(max = 10_000) String descricao,
        // A lista precisa ter itens; cada ID deve ser não nulo. Existência é regra do service.
        @Schema(description = "IDs de públicos-alvo ativos; repetições representam uma única associação.", example = "[1]")
        @NotEmpty List<@NotNull Long> publicoAlvoIds,
        @Schema(description = "ID de uma categoria ativa.", example = "1")
        @NotNull Long categoriaId,
        @Schema(description = "IDs de cursos ativos; repetições representam uma única associação.", example = "[1, 2]")
        @NotEmpty List<@NotNull Long> cursoIds) {
}
