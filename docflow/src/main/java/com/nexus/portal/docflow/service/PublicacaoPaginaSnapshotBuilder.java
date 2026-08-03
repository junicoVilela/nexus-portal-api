package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.dto.response.PaginaResponse;
import com.nexus.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class PublicacaoPaginaSnapshotBuilder {

  private PublicacaoPaginaSnapshotBuilder() {
  }

  static List<PublicacaoPaginaSnapshotItem> build(List<PaginaResponse> paginas) {
    Map<UUID, UUID> parentById = new HashMap<>();
    for (PaginaResponse pagina : paginas) {
      parentById.put(pagina.id(), pagina.parentId());
    }

    List<PublicacaoPaginaSnapshotItem> snapshot = new ArrayList<>(paginas.size());
    for (PaginaResponse pagina : paginas) {
      snapshot.add(new PublicacaoPaginaSnapshotItem(
          pagina.id(),
          pagina.parentId(),
          pagina.titulo(),
          pagina.codigoTela(),
          pagina.slug(),
          pagina.ordem(),
          calcularNivel(pagina.id(), parentById)));
    }
    return List.copyOf(snapshot);
  }

  private static int calcularNivel(UUID id, Map<UUID, UUID> parentById) {
    int nivel = 0;
    UUID parentId = parentById.get(id);
    while (parentId != null && parentById.containsKey(parentId)) {
      nivel++;
      parentId = parentById.get(parentId);
    }
    return nivel;
  }
}
