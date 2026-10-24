package br.com.fatecararas.piapi.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

// Catálogo independente: propostas guardam IDs, sem duplicar nomes.
@Document("categorias")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {
    // Spring Data converte a string hexadecimal para ObjectId no campo _id.
    @Id
    private String id;
    // A unicidade depende de um índice real, criado na inicialização.
    @Indexed(unique = true)
    private String nome;
    private String descricao;
    private boolean ativa = true;
}
