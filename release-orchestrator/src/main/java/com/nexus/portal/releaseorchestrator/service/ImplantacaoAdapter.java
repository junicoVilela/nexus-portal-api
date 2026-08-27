package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.dto.response.ManifestoImplantacaoResponse;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.OperacaoDeploy;
import com.nexus.portal.releaseorchestrator.entity.Release;

/**
 * Adaptador de implantação. Dry-run descreve o contrato; REAL (Docker pull)
 * aplica na Engine API do host.
 */
public interface ImplantacaoAdapter {

  Resultado aplicar(Contexto contexto);

  record Contexto(
      Release release,
      InstalacaoCliente instalacao,
      Host host,
      OperacaoDeploy operacao,
      ManifestoImplantacaoResponse manifesto) {}

  record Resultado(boolean ok, String mensagem, String erro) {
    public static Resultado ok(String mensagem) {
      return new Resultado(true, mensagem, null);
    }

    public static Resultado falha(String erro) {
      return new Resultado(false, null, erro);
    }
  }
}
