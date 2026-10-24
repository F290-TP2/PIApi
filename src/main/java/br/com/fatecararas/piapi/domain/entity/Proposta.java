package br.com.fatecararas.piapi.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import br.com.fatecararas.piapi.domain.StatusProposta;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Uma proposta é um documento; não existem tabelas de junção nem carregamento LAZY.
@Document("propostas")
@CompoundIndex(name = "status_data", def = "{'status': 1, 'criadaEm': -1, '_id': -1}")
@Getter
@Setter
@NoArgsConstructor
public class Proposta {
    @Id
    private String id;
    private String titulo;
    private String resumo;
    private String descricao;
    private StatusProposta status = StatusProposta.PUBLICADA;
    // Referências manuais armazenadas como strings, sem FK ou cascata automática.
    // O serviço valida os IDs e consulta o autor para construir o DTO.
    private String autorId;
    private String categoriaId;
    // Arrays de IDs substituem as tabelas associativas. Set evita repetições.
    private Set<String> cursoIds = new HashSet<>();
    private Set<String> publicoAlvoIds = new HashSet<>();
    // Auditoria substitui @PrePersist/@PreUpdate; alterações exigem save explícito.
    @CreatedDate
    private Instant criadaEm;
    @LastModifiedDate
    private Instant atualizadaEm;
}
