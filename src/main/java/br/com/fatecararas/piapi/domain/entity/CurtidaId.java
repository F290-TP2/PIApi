package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

// Compõe outra entidade: esta classe representa a chave, sem ter tabela própria.
@Embeddable
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
@AllArgsConstructor
// Compara os dois IDs: a mesma dupla representa a mesma chave.
@EqualsAndHashCode
public class CurtidaId implements Serializable {

    private Long usuarioId;
    private Long propostaId;
}
