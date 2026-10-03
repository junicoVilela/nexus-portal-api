package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiDocumentoImportacao;
import com.nexus.portal.ai.entity.AiImportacaoStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDocumentoImportacaoRepository extends JpaRepository<AiDocumentoImportacao, UUID> {

  Optional<AiDocumentoImportacao> findByIdAndCreatedBy(UUID id, String createdBy);

  /** Importações não concluídas do usuário, mais recentes primeiro. */
  List<AiDocumentoImportacao> findTop10ByCreatedByAndStatusNotOrderByUpdatedAtDesc(
      String createdBy, AiImportacaoStatus status);

  /** Mesmo arquivo (hash) com importação ainda em andamento: retomar em vez de duplicar. */
  Optional<AiDocumentoImportacao> findFirstByCreatedByAndHashSha256AndStatusNotOrderByUpdatedAtDesc(
      String createdBy, String hashSha256, AiImportacaoStatus status);
}
