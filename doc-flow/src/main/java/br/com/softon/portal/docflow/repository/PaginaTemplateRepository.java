package br.com.softon.portal.docflow.repository;

import br.com.softon.portal.docflow.entity.PaginaTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaginaTemplateRepository extends JpaRepository<PaginaTemplate, UUID> {
  List<PaginaTemplate> findByAtivoTrueOrderByOrdemAscNomeAsc();

  @Query("""
      select t from PaginaTemplate t
      left join fetch t.projeto
      left join fetch t.cliente
      where (:incluirArquivados = true or t.ativo = true)
        and (:somenteContexto = false
          or t.personalizado = false
          or (:projetoId is not null and t.projeto.id = :projetoId)
          or (:projetoId is not null and t.cliente.id in (
            select cp.cliente.id from ClienteProjeto cp where cp.projeto.id = :projetoId
          ))
          or (:clienteId is not null and t.cliente.id = :clienteId))
      order by t.ativo desc, t.ordem asc, t.nome asc
      """)
  List<PaginaTemplate> listar(@Param("projetoId") UUID projetoId,
      @Param("clienteId") UUID clienteId,
      @Param("somenteContexto") boolean somenteContexto,
      @Param("incluirArquivados") boolean incluirArquivados);
}
