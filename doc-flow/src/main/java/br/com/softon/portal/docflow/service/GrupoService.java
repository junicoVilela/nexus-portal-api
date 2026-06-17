package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.dto.request.GrupoPermissoesRequest;
import br.com.softon.portal.docflow.dto.request.GrupoRequest;
import br.com.softon.portal.docflow.dto.request.GrupoUsuariosRequest;
import br.com.softon.portal.docflow.entity.Grupo;
import br.com.softon.portal.docflow.repository.GrupoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GrupoService {

  private final GrupoRepository grupoRepository;

  @Transactional
  public Grupo criar(GrupoRequest request) {
    String nome = request.nome().trim();
    if (grupoRepository.existsByNomeIgnoreCase(nome)) {
      throw new BusinessException("Já existe um grupo com o nome informado.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    return grupoRepository.save(new Grupo(nome, request.descricao(), ativo));
  }

  @Transactional
  public Grupo atualizar(UUID id, GrupoRequest request) {
    Grupo grupo = buscar(id);
    String nome = request.nome().trim();
    if (grupoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
      throw new BusinessException("Já existe um grupo com o nome informado.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    grupo.atualizar(nome, request.descricao(), ativo);
    return grupo;
  }

  @Transactional
  public Grupo alterarStatus(UUID id, boolean ativo) {
    Grupo grupo = buscar(id);
    grupo.alterarStatus(ativo);
    return grupo;
  }

  @Transactional
  public void excluir(UUID id) {
    Grupo grupo = buscar(id);
    grupoRepository.delete(grupo);
  }

  public Page<Grupo> listar(String nome, Pageable pageable) {
    String filtro = lowerBlankToNull(nome);
    Specification<Grupo> spec = (root, query, cb) -> {
      if (filtro == null) {
        return cb.conjunction();
      }
      return cb.like(cb.lower(root.get("nome")), "%" + filtro + "%");
    };
    return grupoRepository.findAll(spec, pageable);
  }

  public Grupo buscar(UUID id) {
    return grupoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Grupo não encontrado."));
  }

  @Transactional
  public List<UUID> listarMembros(UUID id) {
    return buscar(id).getUsuarios();
  }

  @Transactional
  public void salvarMembros(UUID id, GrupoUsuariosRequest request) {
    Grupo grupo = buscar(id);
    grupo.atualizarUsuarios(request.usuarioIds());
  }

  @Transactional
  public List<String> listarPermissoes(UUID id) {
    return buscar(id).getPermissoes();
  }

  @Transactional
  public void salvarPermissoes(UUID id, GrupoPermissoesRequest request) {
    Grupo grupo = buscar(id);
    grupo.atualizarPermissoes(request.permissoes());
  }

  private String lowerBlankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase();
  }
}
