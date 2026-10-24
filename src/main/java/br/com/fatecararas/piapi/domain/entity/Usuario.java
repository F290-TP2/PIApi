package br.com.fatecararas.piapi.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import br.com.fatecararas.piapi.domain.PerfilUsuario;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;

@Document("usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @Id
    private String id;
    private String nome;
    // Normalizar email antes de persistir; o índice impede duplicação.
    @Indexed(unique = true)
    private String email;
    // Nunca expor este campo nos DTOs. Autenticação entra no tutorial 05.
    private String senhaHash;
    private PerfilUsuario perfil = PerfilUsuario.USER;
    private boolean ativo = true;
    @CreatedDate
    private Instant criadoEm;
}
