package com.nexus.identityaccess.repository;

import com.nexus.identityaccess.entity.PoliticaSenha;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaSenhaRepository extends JpaRepository<PoliticaSenha, UUID> {
}
