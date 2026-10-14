package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repository abstrai o acesso aos dados; Spring Data gera a implementação desta interface.
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrueOrderByNomeAsc();

    Optional<Categoria> findByIdAndAtivaTrue(Long id);
}
