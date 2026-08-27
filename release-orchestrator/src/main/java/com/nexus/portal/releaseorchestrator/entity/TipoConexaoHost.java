package com.nexus.portal.releaseorchestrator.entity;

/** Canal de conexão com o host. Usado nas fases posteriores de deploy. */
public enum TipoConexaoHost {
  SSH,
  WINRM,
  DOCKER
}
