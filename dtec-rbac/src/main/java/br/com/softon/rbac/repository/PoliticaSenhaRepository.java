package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.PoliticaSenha;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaSenhaRepository extends JpaRepository<PoliticaSenha, UUID> {
}
