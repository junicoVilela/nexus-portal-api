package com.nexus.portal.ai.service;

import java.util.List;

public record AiPageSpec(
    int schemaVersion,
    String blueprintId,
    String titulo,
    String slug,
    String codigoTela,
    String resumo,
    List<Bloco> blocos) {

  public record Bloco(String componenteId, List<Texto> textos) {
  }

  public record Texto(String slotId, String valor) {
  }
}
