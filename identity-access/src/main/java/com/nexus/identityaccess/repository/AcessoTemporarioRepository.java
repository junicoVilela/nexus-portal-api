package com.nexus.identityaccess.repository;

import com.nexus.identityaccess.entity.AcessoTemporario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcessoTemporarioRepository
    extends JpaRepository<AcessoTemporario, UUID>, JpaSpecificationExecutor<AcessoTemporario> {

  /**
   * Retorna vínculos ATIVOS (janela aberta e não revogados) de um usuário.
   * Filtra em SQL para não trazer registros irrelevantes ao computar RBAC.
   */
  @Query("SELECT a FROM AcessoTemporario a WHERE a.usuarioId = :usuarioId "
      + "AND a.revogadoEm IS NULL AND a.inicioEm <= :agora AND a.fimEm > :agora")
  List<AcessoTemporario> findAtivosDoUsuario(
      @Param("usuarioId") UUID usuarioId,
      @Param("agora") OffsetDateTime agora);
}
