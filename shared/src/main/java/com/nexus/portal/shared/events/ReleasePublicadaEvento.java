package com.nexus.portal.shared.events;

import java.util.UUID;

/**
 * Release publicada no Release Orchestrator (INT-301). Fica no {@code shared} para o RO avisar sem
 * depender de quem escuta (a fila de propostas da IA marca as telas citadas para revisão).
 *
 * @param texto título, resumo e itens da release — onde os códigos de tela aparecem
 * @param caminho rota da release no portal, para o link na fila
 */
public record ReleasePublicadaEvento(UUID releaseId, String produto, String versao, String titulo, String texto,
    String caminho) {

  public String rotulo() {
    return produto + " " + versao;
  }
}
