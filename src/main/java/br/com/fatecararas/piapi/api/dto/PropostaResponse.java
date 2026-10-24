package br.com.fatecararas.piapi.api.dto;

import br.com.fatecararas.piapi.domain.StatusProposta;

import java.time.Instant;
import java.util.List;

public record PropostaResponse(
        String id,
        String titulo,
        String resumo,
        String descricao,
        List<String> publicoAlvoIds,
        String categoriaId,
        List<String> cursoIds,
        String autorId,
        String autorNome,
        StatusProposta status,
        int quantidadeCurtidas,
        boolean curtidaPeloUsuarioAtual,
        Instant criadaEm,
        Instant atualizadaEm) {
}
