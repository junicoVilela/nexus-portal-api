package com.nexus.portal.docflow.service;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Credencial de leitura do manual (Onda D): chave de integração ({@code nxm_…}) ou link de prévia.
 * Um ponto só para o site hospedado, o help-bridge, as perguntas do leitor e o MCP.
 */
@Service
public class ManualLeitorService {

  private final ManualAcessoService acessoService;
  private final PreviewTokenService previewTokenService;
  private final ManualCorpusService corpusService;

  public ManualLeitorService(
      ManualAcessoService acessoService,
      PreviewTokenService previewTokenService,
      ManualCorpusService corpusService) {
    this.acessoService = acessoService;
    this.previewTokenService = previewTokenService;
    this.corpusService = corpusService;
  }

  /** @param origem cabeçalho {@code Origin} (nulo fora do navegador) */
  public UUID cliente(String token, String origem) {
    return ManualAcessoService.ehChave(token)
        ? acessoService.clienteDaChave(token, origem)
        : previewTokenService.clienteDoToken(token);
  }

  public ManualCorpusService.Corpus corpusVigente(String token, String origem) {
    return corpusService.vigenteDoCliente(cliente(token, origem));
  }
}
