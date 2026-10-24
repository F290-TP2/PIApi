package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Curtida;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CurtidaRepository extends MongoRepository<Curtida, String> {

    long countByPropostaId(String propostaId);
    boolean existsByUsuarioIdAndPropostaId(String usuarioId, String propostaId);
    void deleteByUsuarioIdAndPropostaId(String usuarioId, String propostaId);
}
