package br.com.fatecararas.piapi.application.dto;

import br.com.fatecararas.piapi.domain.model.StatusProposta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

// DTO de saída: expõe os dados da proposta sem serializar as entidades JPA.
@Schema(description = "Proposta devolvida pela API; não expõe entidades nem senha do autor.")
public record PropostaResponse(
        @Schema(description = "Identificador gerado pelo servidor.", example = "1")
        Long id,
        @Schema(description = "Título da proposta.", example = "Horta comunitária conectada")
        String titulo,
        @Schema(description = "Resumo da proposta.", example = "Monitoramento acessível de uma horta local.")
        String resumo,
        @Schema(description = "Descrição detalhada.", example = "Integração de tecnologia e sustentabilidade na comunidade.")
        String descricao,
        @Schema(description = "IDs dos públicos associados.", example = "[1]")
        List<Long> publicoAlvoIds,
        @Schema(description = "ID da categoria.", example = "1")
        Long categoriaId,
        @Schema(description = "IDs dos cursos associados.", example = "[1, 2]")
        List<Long> cursoIds,
        @Schema(description = "Autor definido pelo backend; demonstrativo nesta etapa.", example = "1")
        Long autorId,
        @Schema(description = "Nome público do autor.", example = "Usuário de demonstração")
        String autorNome,
        @Schema(description = "Situação da proposta; consultas públicas retornam PUBLICADA.", example = "PUBLICADA")
        StatusProposta status,
        @Schema(description = "Total de curtidas persistidas.", example = "0")
        int quantidadeCurtidas,
        @Schema(description = "Indica curtida do usuário demonstrativo enquanto não há login.", example = "false")
        boolean curtidaPeloUsuarioAtual,
        @Schema(description = "Instante de criação em UTC.", example = "2026-10-24T12:00:00Z")
        Instant criadaEm,
        @Schema(description = "Instante da última alteração em UTC.", example = "2026-10-24T12:00:00Z")
        Instant atualizadaEm) {
}
