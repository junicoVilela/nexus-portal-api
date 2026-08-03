package com.nexus.identityaccess.repository;

import com.nexus.identityaccess.entity.HistoricoLogin;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HistoricoLoginRepository
    extends JpaRepository<HistoricoLogin, UUID>, JpaSpecificationExecutor<HistoricoLogin> {
}
