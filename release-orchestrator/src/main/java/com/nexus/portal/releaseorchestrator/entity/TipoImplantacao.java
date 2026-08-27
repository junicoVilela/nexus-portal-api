package com.nexus.portal.releaseorchestrator.entity;

/**
 * Como o RPA é implantado no host. Fica na instalação, não no host.
 *
 * <ul>
 *   <li>{@code DOCKER_PULL} — Docker baixando a imagem do registry</li>
 *   <li>{@code DOCKER_TAR} — Docker carregando imagem a partir de arquivo {@code .tar}</li>
 *   <li>{@code LINUX_MANUAL} — sem Docker, instalação manual em Linux</li>
 *   <li>{@code WINDOWS_MANUAL} — instalação manual em Windows</li>
 * </ul>
 */
public enum TipoImplantacao {
  DOCKER_PULL,
  DOCKER_TAR,
  LINUX_MANUAL,
  WINDOWS_MANUAL
}
