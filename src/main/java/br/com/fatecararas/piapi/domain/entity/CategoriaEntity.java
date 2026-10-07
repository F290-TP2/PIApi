package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Table(name = "categorias")
@Entity
public class CategoriaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 80, nullable = false)
    private String nome;
    @Column(length = 300)
    private String descricao;
    private Boolean ativa = true;
}
