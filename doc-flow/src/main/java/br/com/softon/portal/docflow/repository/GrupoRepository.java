package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.Grupo;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GrupoRepository extends JpaRepository<Grupo, UUID>, JpaSpecificationExecutor<Grupo> {

  boolean existsByNomeIgnoreCase(String nome);

  boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);
}
