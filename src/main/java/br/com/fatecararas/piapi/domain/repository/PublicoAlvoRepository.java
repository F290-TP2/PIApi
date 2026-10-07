package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.PublicoAlvo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicoAlvoRepository extends JpaRepository<PublicoAlvo, Long> {

    List<PublicoAlvo> findByAtivoTrueOrderByNomeAsc();

    Optional<PublicoAlvo> findByIdAndAtivoTrue(Long id);
}
