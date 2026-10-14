package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Curtida;
import br.com.fatecararas.piapi.domain.entity.CurtidaId;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository abstrai o acesso aos dados; Spring Data gera a implementação desta interface.
public interface CurtidaRepository extends JpaRepository<Curtida, CurtidaId> {

    // Consulta derivada: percorre Curtida.id.propostaId e conta os registros.
    long countByIdPropostaId(Long propostaId);
}
