package br.com.fatecararas.piapi.api.dto;

import br.com.fatecararas.piapi.domain.StatusProposta;

import java.time.Instant;
import java.util.List;

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
