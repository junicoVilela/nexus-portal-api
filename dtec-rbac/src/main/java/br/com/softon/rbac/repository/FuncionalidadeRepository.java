package br.com.softon.rbac.repository;

import br.com.softon.rbac.entity.Funcionalidade;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionalidadeRepository extends JpaRepository<Funcionalidade, UUID> {
}
