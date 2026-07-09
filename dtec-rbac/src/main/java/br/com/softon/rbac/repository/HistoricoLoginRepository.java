package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.HistoricoLogin;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HistoricoLoginRepository
    extends JpaRepository<HistoricoLogin, UUID>, JpaSpecificationExecutor<HistoricoLogin> {
}
