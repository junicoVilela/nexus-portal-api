package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.Publicacao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicacaoRepository extends JpaRepository<Publicacao, UUID> {
  boolean existsByCliente_Id(UUID clienteId);

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
  Page<Publicacao> findByCliente_IdIn(Collection<UUID> clienteIds, Pageable pageable);

  @EntityGraph(attributePaths = "cliente")
  List<Publicacao> findByCliente_IdInOrderByCreatedAtDesc(Collection<UUID> clienteIds);

  @Override
  @EntityGraph(attributePaths = "cliente")
  Page<Publicacao> findAll(Pageable pageable);
}
