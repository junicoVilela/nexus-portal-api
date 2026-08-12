package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiSessao;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiSessaoRepository extends JpaRepository<AiSessao, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from AiSessao s where s.id = :id")
  Optional<AiSessao> findByIdForUpdate(@Param("id") UUID id);
}
