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

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "curtida")
@Getter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Curtida {

    // A chave primária é o par (usuarioId, propostaId), definido em CurtidaId.
    @EmbeddedId
    private CurtidaId id;

    // Usa o ID da entidade relacionada também no campo usuarioId da chave composta.
    @MapsId("usuarioId")
    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Usa o ID da entidade relacionada também no campo propostaId da chave composta.
    @MapsId("propostaId")
    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "proposta_id")
    private Proposta proposta;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    // Recebe usuário e proposta já persistidos, com IDs definidos.
    public Curtida(Usuario usuario, Proposta proposta) {
        this.id = new CurtidaId(usuario.getId(), proposta.getId());
        this.usuario = usuario;
        this.proposta = proposta;
        this.criadaEm = Instant.now();
    }
}
