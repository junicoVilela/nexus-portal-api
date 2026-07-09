package br.com.softon.rbac.service;

import br.com.softon.rbac.dto.response.GrupoMeResponse;
import br.com.softon.rbac.entity.Grupo;
import br.com.softon.rbac.entity.Permissao;
import br.com.softon.rbac.repository.GrupoRepository;
import br.com.softon.rbac.repository.PermissaoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RbacService {

  private final GrupoRepository grupoRepository;
  private final PermissaoRepository permissaoRepository;

  public List<GrupoMeResponse> gruposDoUsuario(UUID usuarioId) {
    return grupoRepository.findAtivosComUsuario(usuarioId).stream()
        .sorted(Comparator.comparing(Grupo::getCodigo))
        .map(g -> new GrupoMeResponse(g.getId(), g.getCodigo(), g.getNome()))
        .toList();
  }

  public List<String> permissoesDoUsuario(UUID usuarioId) {
    Set<UUID> permissaoIds = new LinkedHashSet<>();
    for (Grupo grupo : grupoRepository.findAtivosComUsuario(usuarioId)) {
      permissaoIds.addAll(grupo.getPermissaoIds());
    }
    if (permissaoIds.isEmpty()) {
      return List.of();
    }
    return permissaoRepository.findByIdIn(permissaoIds).stream()
        .filter(Permissao::isAtivo)
        .map(Permissao::getCodigo)
        .sorted()
        .toList();
  }

  public List<String> codigosPermissoesDoGrupo(Grupo grupo) {
    if (grupo.getPermissaoIds().isEmpty()) {
      return List.of();
    }
    return permissaoRepository.findByIdIn(grupo.getPermissaoIds()).stream()
        .filter(Permissao::isAtivo)
        .map(Permissao::getCodigo)
        .sorted()
        .toList();
  }

  /**
   * Resolve UUIDs de permissão a partir de uma lista que pode conter tanto
   * códigos (formato FUNCIONALIDADE:ACAO) quanto UUIDs. Aceita mistura.
   * Permite ao frontend enviar IDs (que já tem em cache) sem exigir que
   * ele conheça o código canônico.
   */
  public List<UUID> idsPorCodigos(List<String> codigosOuIds) {
    if (codigosOuIds == null || codigosOuIds.isEmpty()) {
      return List.of();
    }
    Set<String> distintos = new LinkedHashSet<>(codigosOuIds);
    Set<UUID> uuids = new LinkedHashSet<>();
    Set<String> codigos = new LinkedHashSet<>();
    for (String valor : distintos) {
      UUID uuid = tentarUuid(valor);
      if (uuid != null) {
        uuids.add(uuid);
      } else {
        codigos.add(valor);
      }
    }

    Set<UUID> resolvidos = new LinkedHashSet<>();
    if (!uuids.isEmpty()) {
      List<Permissao> porId = permissaoRepository.findByIdIn(uuids);
      if (porId.size() != uuids.size()) {
        throw new BusinessException(
            "Uma ou mais permissões informadas não existem no catálogo.");
      }
      porId.forEach(p -> resolvidos.add(p.getId()));
    }
    if (!codigos.isEmpty()) {
      List<Permissao> porCodigo = permissaoRepository.findByCodigoIn(codigos);
      if (porCodigo.size() != codigos.size()) {
        throw new BusinessException(
            "Uma ou mais permissões informadas não existem no catálogo.");
      }
      porCodigo.forEach(p -> resolvidos.add(p.getId()));
    }
    return List.copyOf(resolvidos);
  }

  private static UUID tentarUuid(String valor) {
    try {
      return UUID.fromString(valor);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }
}
