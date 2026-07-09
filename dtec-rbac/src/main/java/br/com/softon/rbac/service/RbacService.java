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

  public List<UUID> idsPorCodigos(List<String> codigos) {
    if (codigos == null || codigos.isEmpty()) {
      return List.of();
    }
    Set<String> distintos = new LinkedHashSet<>(codigos);
    List<Permissao> encontradas = permissaoRepository.findByCodigoIn(distintos);
    if (encontradas.size() != distintos.size()) {
      throw new BusinessException(
          "Uma ou mais permissões informadas não existem no catálogo.");
    }
    return encontradas.stream().map(Permissao::getId).toList();
  }
}
