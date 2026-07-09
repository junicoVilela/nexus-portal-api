package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.Grupo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GrupoRepository extends JpaRepository<Grupo, UUID>, JpaSpecificationExecutor<Grupo> {

  boolean existsByNomeIgnoreCase(String nome);

  boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

  @Query("SELECT g.codigo FROM Grupo g WHERE upper(g.codigo) LIKE upper(concat(:prefixo, '%'))")
  List<String> findCodigosComPrefixo(@Param("prefixo") String prefixo);

  @Query("SELECT g FROM Grupo g WHERE g.ativo = true AND :usuarioId MEMBER OF g.usuarios")
  List<Grupo> findAtivosComUsuario(@Param("usuarioId") UUID usuarioId);
}
