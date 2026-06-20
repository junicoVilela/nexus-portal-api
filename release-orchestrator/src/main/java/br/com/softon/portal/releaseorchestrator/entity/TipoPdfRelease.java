package br.com.softon.portal.releaseorchestrator.entity;

import java.util.Set;

/**
 * Modos de geração do PDF da release. Cada tipo define quais visibilidades
 * de item são incluídas (spec 25 §2).
 */
public enum TipoPdfRelease {

  /** Cliente final — só itens com visibilidade TODOS. */
  CLIENTE(Set.of(VisibilidadeItem.TODOS)),

  /** Suporte — TODOS + SUPORTE. */
  SUPORTE(Set.of(VisibilidadeItem.TODOS, VisibilidadeItem.SUPORTE)),

  /** Interno — qualquer visibilidade. */
  INTERNO(Set.of(VisibilidadeItem.TODOS, VisibilidadeItem.TECNICO, VisibilidadeItem.SUPORTE));

  private final Set<VisibilidadeItem> visibilidadesPermitidas;

  TipoPdfRelease(Set<VisibilidadeItem> visibilidadesPermitidas) {
    this.visibilidadesPermitidas = visibilidadesPermitidas;
  }

  public boolean inclui(VisibilidadeItem visibilidade) {
    return visibilidadesPermitidas.contains(visibilidade);
  }
}
