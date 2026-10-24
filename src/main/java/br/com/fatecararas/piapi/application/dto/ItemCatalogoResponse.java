package br.com.fatecararas.piapi.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item ativo de catálogo. Descrição e sigla podem ser nulas conforme o catálogo.")
public record ItemCatalogoResponse(
        @Schema(description = "ID usado nos filtros e no corpo da proposta.", example = "1") Long id,
        @Schema(description = "Nome do item.", example = "Tecnologia e inovação") String nome,
        @Schema(description = "Descrição, quando disponível.", example = "Soluções digitais e novos produtos.") String descricao,
        @Schema(description = "Sigla apenas para cursos, quando informada.", example = "DSM") String sigla) {
}
