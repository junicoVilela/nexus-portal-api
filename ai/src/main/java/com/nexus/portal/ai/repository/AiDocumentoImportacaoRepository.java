package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDocumentoImportacaoRepository extends JpaRepository<AiDocumentoImportacao, UUID> {

  Optional<AiDocumentoImportacao> findByIdAndCreatedBy(UUID id, String createdBy);
}
