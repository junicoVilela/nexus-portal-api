package com.nexus.portal.ai.entity;

/** De onde veio o item da fila de propostas (INT-303). */
public enum AiFilaOrigem {
  /** PR mergeado no GitHub (Fase C). */
  PR,
  /** Release publicada no Release Orchestrator (INT-301). */
  RELEASE
}
