package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Proposta;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PropostaRepository extends MongoRepository<Proposta, String> {
}
