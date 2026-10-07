package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "curtida")
@Getter
@NoArgsConstructor
public class Curtida {

    @EmbeddedId
    private CurtidaId id;

    @MapsId("usuarioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @MapsId("propostaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposta_id")
    private Proposta proposta;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    public Curtida(Usuario usuario, Proposta proposta) {
        this.id = new CurtidaId(usuario.getId(), proposta.getId());
        this.usuario = usuario;
        this.proposta = proposta;
        this.criadaEm = Instant.now();
    }
}
