package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.Dominio;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DominioRepository extends JpaRepository<Dominio, UUID> {
}
