package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.Sessao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SessaoRepository extends JpaRepository<Sessao, UUID>, JpaSpecificationExecutor<Sessao> {

  Optional<Sessao> findByJti(String jti);
}
