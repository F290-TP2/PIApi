package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repository abstrai o acesso aos dados; Spring Data gera a implementação desta interface.
public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByAtivoTrueOrderByNomeAsc();
}
