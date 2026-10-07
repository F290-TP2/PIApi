package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
