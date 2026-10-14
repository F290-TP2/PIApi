package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "categoria")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Categoria {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; unique=true impede valores repetidos; length define o tamanho da coluna de texto.
    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    // @Column: length define o tamanho da coluna de texto.
    @Column(length = 300)
    private String descricao;

    // @Column: nullable=false impede NULL.
    @Column(nullable = false)
    private boolean ativa = true;
}
