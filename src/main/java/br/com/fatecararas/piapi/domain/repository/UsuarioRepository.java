package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository abstrai o acesso aos dados; Spring Data gera a implementação desta interface.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
