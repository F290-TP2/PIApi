package br.com.fatecararas.piapi.domain.entity;

import br.com.fatecararas.piapi.domain.StatusProposta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "proposta")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Proposta {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 120)
    private String titulo;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 300)
    private String resumo;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 10000)
    private String descricao;

    // Grava o nome do enum (texto), em vez de sua posição numérica.
    @Enumerated(EnumType.STRING)
    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 20)
    private StatusProposta status = StatusProposta.PUBLICADA;

    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // N:M: propostas compartilham itens do catálogo por uma tabela de ligação.
    // joinColumns aponta para Proposta; inverseJoinColumns, para o item do catálogo.
    @ManyToMany
    @JoinTable(name = "proposta_curso",
            joinColumns = @JoinColumn(name = "proposta_id"),
            inverseJoinColumns = @JoinColumn(name = "curso_id"))
    private Set<Curso> cursos = new HashSet<>();

    // N:M: propostas compartilham itens do catálogo por uma tabela de ligação.
    // joinColumns aponta para Proposta; inverseJoinColumns, para o item do catálogo.
    @ManyToMany
    @JoinTable(name = "proposta_publico_alvo",
            joinColumns = @JoinColumn(name = "proposta_id"),
            inverseJoinColumns = @JoinColumn(name = "publico_alvo_id"))
    private Set<PublicoAlvo> publicosAlvo = new HashSet<>();

    // @Column: name define o nome da coluna; nullable=false impede NULL; updatable=false exclui a coluna dos UPDATEs do JPA.
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    // O JPA chama este método antes de inserir a entidade.
    @PrePersist
    void antesDeInserir() {
        Instant agora = Instant.now();
        criadaEm = agora;
        atualizadaEm = agora;
        if (status == null) {
            status = StatusProposta.PUBLICADA;
        }
    }

    // O JPA chama este método antes de atualizar uma entidade alterada.
    @PreUpdate
    void antesDeAtualizar() {
        atualizadaEm = Instant.now();
    }
}
