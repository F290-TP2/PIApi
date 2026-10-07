package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Curtida;
import br.com.fatecararas.piapi.domain.entity.CurtidaId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurtidaRepository extends JpaRepository<Curtida, CurtidaId> {

    long countByIdPropostaId(Long propostaId);
}
