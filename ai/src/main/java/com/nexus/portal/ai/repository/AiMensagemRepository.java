package com.nexus.portal.ai.repository;

import com.nexus.portal.ai.entity.AiMensagem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMensagemRepository extends JpaRepository<AiMensagem, UUID> {

  List<AiMensagem> findBySessaoIdOrderByOrdemAsc(UUID sessaoId);

  int countBySessaoId(UUID sessaoId);
}
