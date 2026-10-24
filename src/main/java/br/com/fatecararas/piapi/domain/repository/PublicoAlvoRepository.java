package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.PublicoAlvo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PublicoAlvoRepository extends MongoRepository<PublicoAlvo, String> {

    List<PublicoAlvo> findByAtivoTrueOrderByNomeAsc();

    Optional<PublicoAlvo> findByIdAndAtivoTrue(String id);
}
