package br.com.fatecararas.piapi.domain.entity;

import br.com.fatecararas.piapi.domain.model.PerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "usuario")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Usuario {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 120)
    private String nome;

    // @Column: nullable=false impede NULL; unique=true impede valores repetidos; length define o tamanho da coluna de texto.
    @Column(nullable = false, unique = true, length = 180)
    private String email;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    // Grava o nome do enum (texto), em vez de sua posição numérica.
    @Enumerated(EnumType.STRING)
    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil = PerfilUsuario.USER;

    // @Column: nullable=false impede NULL.
    @Column(nullable = false)
    private boolean ativo = true;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    // O JPA chama este método antes de inserir a entidade.
    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
        if (perfil == null) {
            perfil = PerfilUsuario.USER;
        }
    }
}
