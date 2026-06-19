package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.Permissao;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoRepository extends JpaRepository<Permissao, UUID> {

  List<Permissao> findByCodigoIn(Collection<String> codigos);

  List<Permissao> findByIdIn(Collection<UUID> ids);
}
