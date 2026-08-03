package com.nexus.identityaccess.repository;

import com.nexus.identityaccess.entity.Funcionalidade;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionalidadeRepository extends JpaRepository<Funcionalidade, UUID> {
}
