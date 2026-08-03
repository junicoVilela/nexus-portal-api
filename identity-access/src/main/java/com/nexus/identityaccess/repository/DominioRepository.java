package com.nexus.identityaccess.repository;

import com.nexus.identityaccess.entity.Dominio;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DominioRepository extends JpaRepository<Dominio, UUID> {
}
