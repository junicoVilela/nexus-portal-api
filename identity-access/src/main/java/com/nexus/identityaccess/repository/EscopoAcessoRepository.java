package com.nexus.identityaccess.repository;

import com.nexus.identityaccess.entity.EscopoAcesso;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EscopoAcessoRepository
    extends JpaRepository<EscopoAcesso, UUID>, JpaSpecificationExecutor<EscopoAcesso> {

  List<EscopoAcesso> findByUsuarioIdAndAtivoTrue(UUID usuarioId);

  List<EscopoAcesso> findByGrupoIdAndAtivoTrue(UUID grupoId);
}
