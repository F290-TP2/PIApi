package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PropostaRepository extends JpaRepository<Proposta, Long>,
        JpaSpecificationExecutor<Proposta> {
}
