package com.nexus.portal.docflow.repository;

import com.nexus.portal.docflow.entity.AjudaConteudo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AjudaConteudoRepository extends JpaRepository<AjudaConteudo, UUID> {
  List<AjudaConteudo> findAllByOrderByOrdemAscTituloAsc();

  List<AjudaConteudo> findByAtivoTrueOrderByOrdemAscTituloAsc();

  Optional<AjudaConteudo> findByCodigo(String codigo);

  boolean existsByCodigo(String codigo);

  boolean existsByJornadaCodigo(String jornadaCodigo);
}
