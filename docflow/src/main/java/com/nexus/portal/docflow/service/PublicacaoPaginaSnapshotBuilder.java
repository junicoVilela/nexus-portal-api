package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.dto.response.PaginaResponse;
import com.nexus.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
          calcularNivel(pagina.id(), parentById),
          hashDoConteudo(pagina.conteudoHtml())));
    }
    return List.copyOf(snapshot);
  }

  /** Resumo curto e estável do conteúdo, para comparar duas publicações. */
  static String hashDoConteudo(String conteudoHtml) {
    if (conteudoHtml == null || conteudoHtml.isBlank()) {
      return null;
    }
    try {
      byte[] resumo = MessageDigest.getInstance("SHA-256")
          .digest(conteudoHtml.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(resumo.length * 2);
      for (byte b : resumo) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", ex);
    }
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
