package com.nexus.identityaccess.service;

import com.nexus.identityaccess.dto.request.GrupoPermissoesRequest;
import com.nexus.identityaccess.dto.request.GrupoRequest;
import com.nexus.identityaccess.dto.request.GrupoUsuariosRequest;
import com.nexus.identityaccess.entity.Grupo;
import com.nexus.identityaccess.repository.GrupoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GrupoService {

  private final GrupoRepository grupoRepository;
  private final RbacService rbacService;
  private final AuditoriaService auditoriaService;

  @Transactional
  public Grupo criar(GrupoRequest request, Principal principal) {
    String nome = request.nome().trim();
    if (grupoRepository.existsByNomeIgnoreCase(nome)) {
      throw new BusinessException("Já existe um grupo com o nome informado.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    Grupo grupo = grupoRepository.save(new Grupo(gerarCodigoUnico(nome), nome, request.descricao(), ativo));
    auditoriaService.registrar("Grupo", grupo.getId(), "CRIAR",
        "Grupo criado: " + grupo.getNome(), principal);
    return grupo;
  }

  @Transactional
  public Grupo atualizar(UUID id, GrupoRequest request, Principal principal) {
    Grupo grupo = buscar(id);
    String nome = request.nome().trim();
    if (grupoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
      throw new BusinessException("Já existe um grupo com o nome informado.");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    grupo.atualizar(nome, request.descricao(), ativo);
    auditoriaService.registrar("Grupo", grupo.getId(), "EDITAR",
        "Grupo atualizado: " + grupo.getNome(), principal);
    return grupo;
  }

  @Transactional
  public Grupo alterarStatus(UUID id, boolean ativo, Principal principal) {
    Grupo grupo = buscar(id);
    grupo.alterarStatus(ativo);
    auditoriaService.registrar("Grupo", id, ativo ? "ATIVAR" : "DESATIVAR",
        "Grupo " + grupo.getNome() + (ativo ? " ativado." : " desativado."), principal);
    return grupo;
  }

  @Transactional
  public void excluir(UUID id, Principal principal) {
    Grupo grupo = buscar(id);
    String nome = grupo.getNome();
    grupoRepository.delete(grupo);
    auditoriaService.registrar("Grupo", id, "EXCLUIR", "Grupo excluído: " + nome, principal);
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
  public void salvarMembros(UUID id, GrupoUsuariosRequest request, Principal principal) {
    Grupo grupo = buscar(id);
    grupo.atualizarUsuarios(request.usuarioIds());
    auditoriaService.registrar("Grupo", id, "VINCULAR_USUARIOS",
        "Membros do grupo " + grupo.getNome() + " atualizados (" + request.usuarioIds().size() + ").",
        principal);
  }

  @Transactional
  public List<String> listarPermissoes(UUID id) {
    return rbacService.codigosPermissoesDoGrupo(buscar(id));
  }

  @Transactional
  public void salvarPermissoes(UUID id, GrupoPermissoesRequest request, Principal principal) {
    Grupo grupo = buscar(id);
    grupo.atualizarPermissaoIds(rbacService.idsPorCodigos(request.permissoes()));
    auditoriaService.registrar("Grupo", id, "VINCULAR_PERMISSAO",
        "Permissões do grupo " + grupo.getNome() + " atualizadas (" + request.permissoes().size() + ").",
        principal);
  }

  private String gerarCodigoUnico(String nome) {
    String base = nome.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    if (base.isBlank()) {
      base = "GRUPO";
    }
    Set<String> existentes = grupoRepository.findCodigosComPrefixo(base).stream()
        .map(c -> c.toUpperCase(Locale.ROOT))
        .collect(Collectors.toSet());
    if (!existentes.contains(base)) {
      return base;
    }
    for (int sufixo = 1; ; sufixo++) {
      String candidato = base + "_" + sufixo;
      if (!existentes.contains(candidato)) {
        return candidato;
      }
    }
  }

  private String lowerBlankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase();
  }
}
