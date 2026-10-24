package br.com.fatecararas.piapi.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Página de resultados. Uma página vazia também retorna HTTP 200.")
public record PaginaResponse<T>(
        @Schema(description = "Itens desta página; no máximo 10.") List<T> conteudo,
        @Schema(description = "Índice da página, começando em zero.", example = "0") int pagina,
        @Schema(description = "Capacidade fixa da página, não a quantidade retornada.", example = "10") int tamanho,
        @Schema(description = "Total de propostas que atendem aos filtros.", example = "1") long totalElementos,
        @Schema(description = "Total de páginas disponíveis.", example = "1") int totalPaginas) {
}
