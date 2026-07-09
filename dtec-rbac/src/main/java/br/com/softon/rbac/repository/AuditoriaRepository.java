package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.AuditoriaEvento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditoriaRepository
    extends JpaRepository<AuditoriaEvento, UUID>, JpaSpecificationExecutor<AuditoriaEvento> {

  List<AuditoriaEvento> findTop100ByOrderByCreatedAtDesc();

  Page<AuditoriaEvento> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
