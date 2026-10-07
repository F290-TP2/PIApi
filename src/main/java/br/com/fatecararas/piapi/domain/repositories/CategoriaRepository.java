package br.com.fatecararas.piapi.domain.repositories;

import br.com.fatecararas.piapi.domain.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Long> {
}
