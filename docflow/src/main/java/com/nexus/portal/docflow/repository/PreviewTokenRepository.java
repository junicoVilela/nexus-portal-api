package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.PreviewToken;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreviewTokenRepository extends JpaRepository<PreviewToken, UUID> {
  Optional<PreviewToken> findByTokenAndAtivoTrue(String token);
  List<PreviewToken> findByClienteIdAndAtivoTrue(UUID clienteId);
  void deleteByClienteId(UUID clienteId);
  long deleteByExpiresAtBefore(OffsetDateTime limite);
}
