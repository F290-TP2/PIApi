package br.com.fatecararas.piapi.domain;

import java.time.Instant;
import java.util.List;

public record Proposta(
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
        Instant criadaEm,
        Instant atualizadaEm) {
}
