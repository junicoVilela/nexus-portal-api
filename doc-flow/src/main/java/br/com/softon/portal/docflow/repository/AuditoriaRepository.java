package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.AuditoriaEvento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<AuditoriaEvento, UUID> {

  List<AuditoriaEvento> findTop100ByOrderByCreatedAtDesc();

  Page<AuditoriaEvento> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
