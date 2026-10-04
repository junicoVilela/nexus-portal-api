package com.nexus.portal.ai.dto.response;

import com.nexus.portal.ai.entity.AiManualPergunta.Modo;
import java.util.List;

/**
 * Resposta do manual (Onda E). {@code modo}: IA (texto gerado com citação), TRECHOS (sem IA, só os
 * trechos) ou NAO_SEI (o manual publicado não cobre a pergunta).
 */
public record AiManualRespostaResponse(
    String manual,
    String versao,
    Modo modo,
    String resposta,
    List<Citacao> citacoes) {

  public record Citacao(String codigoTela, String titulo, String secao, String caminho, String url, String trecho) {}

  public boolean encontrou() {
    return modo != Modo.NAO_SEI;
  }
}
