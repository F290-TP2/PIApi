package br.com.fatecararas.piapi.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Coleção separada evita um array de curtidas sem limite dentro da proposta.
@Document("curtidas")
@CompoundIndex(name = "usuario_proposta_unicos", def = "{'usuarioId': 1, 'propostaId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
public class Curtida {
    @Id
    private String id;
    // O índice composto garante a regra mesmo sob requisições concorrentes.
    private String usuarioId;
    @Indexed
    private String propostaId;
    @CreatedDate
    private Instant criadaEm;

    public Curtida(String usuarioId, String propostaId) {
        this.usuarioId = usuarioId;
        this.propostaId = propostaId;
    }
}
