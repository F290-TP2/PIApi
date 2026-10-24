package br.com.fatecararas.piapi.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

// Catálogo independente: propostas guardam IDs, sem duplicar nomes.
@Document("cursos")
@Getter
@Setter
@NoArgsConstructor
public class Curso {
    // Spring Data converte a string hexadecimal para ObjectId no campo _id.
    @Id
    private String id;
    // A unicidade depende de um índice real, criado na inicialização.
    @Indexed(unique = true)
    private String nome;
    // O índice parcial permite várias siglas ausentes, mas não strings repetidas.
    @Indexed(unique = true, partialFilter = "{ 'sigla': { '$type': 'string' } }")
    private String sigla;
    private boolean ativo = true;
}
