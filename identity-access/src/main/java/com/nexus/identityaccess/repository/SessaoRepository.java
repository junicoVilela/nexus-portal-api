package com.nexus.identityaccess.repository;

import com.nexus.identityaccess.entity.Sessao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SessaoRepository extends JpaRepository<Sessao, UUID>, JpaSpecificationExecutor<Sessao> {

  Optional<Sessao> findByJti(String jti);
}
