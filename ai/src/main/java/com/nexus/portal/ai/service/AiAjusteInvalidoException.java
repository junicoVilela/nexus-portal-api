package com.nexus.portal.ai.service;

/** Falha do ajuste de página cuja mensagem é segura e útil para o autor (ex.: página mudou). */
public class AiAjusteInvalidoException extends RuntimeException {

  public AiAjusteInvalidoException(String mensagem) {
    super(mensagem);
  }
}
