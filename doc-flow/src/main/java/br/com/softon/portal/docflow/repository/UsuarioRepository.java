package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.Usuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
  Optional<Usuario> findByUsernameAndAtivoTrue(String username);
  boolean existsByUsername(String username);
  Page<Usuario> findAllByOrderByUsernameAsc(Pageable pageable);
}
