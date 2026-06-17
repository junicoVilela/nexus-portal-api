package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.PublicacaoChangelog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacaoChangelogRepository extends JpaRepository<PublicacaoChangelog, UUID> {
  List<PublicacaoChangelog> findByPublicacaoIdOrderByCreatedAtAsc(UUID publicacaoId);
}
