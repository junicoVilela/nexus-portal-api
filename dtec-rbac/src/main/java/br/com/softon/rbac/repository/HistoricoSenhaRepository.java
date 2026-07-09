package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.HistoricoSenha;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoSenhaRepository extends JpaRepository<HistoricoSenha, UUID> {

  List<HistoricoSenha> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);
}
