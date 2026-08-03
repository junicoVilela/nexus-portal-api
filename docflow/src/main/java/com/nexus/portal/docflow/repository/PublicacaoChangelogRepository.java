package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.PublicacaoChangelog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacaoChangelogRepository extends JpaRepository<PublicacaoChangelog, UUID> {
  List<PublicacaoChangelog> findByPublicacaoIdOrderByCreatedAtAsc(UUID publicacaoId);
}
