package br.com.fatecararas.piapi.application.dto;

import br.com.fatecararas.piapi.domain.model.StatusProposta;

import java.time.Instant;
import java.util.List;

// DTO de saída: expõe os dados da proposta sem serializar as entidades JPA.
public record PropostaResponse(
        Long id,
        String titulo,
        String resumo,
        String descricao,
        List<Long> publicoAlvoIds,
        Long categoriaId,
        List<Long> cursoIds,
        Long autorId,
        String autorNome,
        StatusProposta status,
        int quantidadeCurtidas,
        boolean curtidaPeloUsuarioAtual,
        Instant criadaEm,
        Instant atualizadaEm) {
}
