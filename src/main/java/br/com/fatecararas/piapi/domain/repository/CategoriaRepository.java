package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Categoria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends MongoRepository<Categoria, String> {

    List<Categoria> findByAtivaTrueOrderByNomeAsc();

    Optional<Categoria> findByIdAndAtivaTrue(String id);
}
