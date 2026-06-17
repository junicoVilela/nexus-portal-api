package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.PreviewToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreviewTokenRepository extends JpaRepository<PreviewToken, UUID> {
  Optional<PreviewToken> findByTokenAndAtivoTrue(String token);
  List<PreviewToken> findByClienteIdAndAtivoTrue(UUID clienteId);
}
