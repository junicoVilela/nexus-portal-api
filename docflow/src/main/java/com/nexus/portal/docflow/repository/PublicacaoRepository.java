package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PublicacaoRepository extends JpaRepository<Publicacao, UUID> {
  long countByStatus(StatusPublicacao status);

  @Query("""
      select count(c) from Cliente c
      where c.ativo = true
        and not exists (
          select p.id from Publicacao p
          where p.cliente = c and p.status = com.nexus.portal.docflow.entity.StatusPublicacao.SUCESSO
        )
      """)
  long countClientesAtivosSemPublicacaoSucesso();

  @EntityGraph(attributePaths = "cliente")
  List<Publicacao> findByStatusAndUpdatedAtBefore(StatusPublicacao status, OffsetDateTime limite);

  boolean existsByCliente_Id(UUID clienteId);

  boolean existsByCliente_IdAndVersao(UUID clienteId, String versao);

  @Override
  @EntityGraph(attributePaths = "cliente")
  Optional<Publicacao> findById(UUID id);

  @EntityGraph(attributePaths = "cliente")
  List<Publicacao> findByCliente_IdOrderByCreatedAtDesc(UUID clienteId);

  @EntityGraph(attributePaths = "cliente")
  List<Publicacao> findAllByOrderByCreatedAtDesc();

  @EntityGraph(attributePaths = "cliente")
  Page<Publicacao> findByCliente_Id(UUID clienteId, Pageable pageable);

  @EntityGraph(attributePaths = "cliente")
  Page<Publicacao> findByStatus(StatusPublicacao status, Pageable pageable);

  @EntityGraph(attributePaths = "cliente")
  Page<Publicacao> findByCliente_IdAndStatus(UUID clienteId, StatusPublicacao status, Pageable pageable);

  @EntityGraph(attributePaths = "cliente")
  Page<Publicacao> findByCliente_IdIn(Collection<UUID> clienteIds, Pageable pageable);

  @EntityGraph(attributePaths = "cliente")
  Page<Publicacao> findByCliente_IdInAndStatus(
      Collection<UUID> clienteIds, StatusPublicacao status, Pageable pageable);

  @EntityGraph(attributePaths = "cliente")
  List<Publicacao> findByCliente_IdInOrderByCreatedAtDesc(Collection<UUID> clienteIds);

  @Override
  @EntityGraph(attributePaths = "cliente")
  Page<Publicacao> findAll(Pageable pageable);
}
