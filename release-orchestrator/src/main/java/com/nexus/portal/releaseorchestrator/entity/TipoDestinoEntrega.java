package com.nexus.portal.releaseorchestrator.entity;

/** Destino físico das entregas. No MVP só PASTA é validado. */
public enum TipoDestinoEntrega {
  PASTA,
  FTP,
  SFTP,
  BUCKET
}
