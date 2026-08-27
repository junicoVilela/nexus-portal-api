package com.nexus.portal.releaseorchestrator.dto.request;

/**
 * Origem do ref que o Jenkins deve compilar ao disparar o job pela ficha
 * da release.
 */
public enum OrigemBuild {
  /** Versão da release aberta no portal ({@code v} + {@code release.versao}). */
  RELEASE_ATUAL,
  /** Tag Git escolhida (ou digitada) pelo operador. */
  TAG_ESPECIFICA,
  /** GitHub Release publicada mais recente do repositório do produto. */
  ULTIMA_GERADA
}
