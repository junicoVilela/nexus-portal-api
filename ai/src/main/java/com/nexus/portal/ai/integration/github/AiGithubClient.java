package com.nexus.portal.ai.integration.github;

import java.util.List;

/** Leitura mínima de PRs no GitHub (Fase C). Sem dependência do Release Orchestrator. */
public interface AiGithubClient {

  /** Arquivos alterados no PR, até {@code limite} (a API pagina de 100 em 100). */
  List<ArquivoPr> arquivos(String repositorio, int numero, int limite);

  /**
   * @param status {@code added}, {@code modified}, {@code removed}, {@code renamed}…
   * @param patch diff unificado do arquivo; nulo em binários ou diffs grandes demais
   */
  record ArquivoPr(String caminho, String status, int adicoes, int remocoes, String patch) {}

  class GithubIndisponivelException extends RuntimeException {
    public GithubIndisponivelException(String mensagem) {
      super(mensagem);
    }

    public GithubIndisponivelException(String mensagem, Throwable causa) {
      super(mensagem, causa);
    }
  }
}
