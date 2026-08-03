package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Cria uma Entrega em RASCUNHO. Se {@code proximaEntregaId} for informado,
 * a ProximaEntrega é convertida (status CONVERTIDA) e cliente/produto/release/
 * ambiente são puxados dela (campos do request são ignorados nesse caso).
 */
public record CriarEntregaRequest(
    UUID proximaEntregaId,
    UUID clienteId,
    UUID produtoId,
    UUID releaseId,
    AmbientePadrao ambiente,
    UUID responsavelId,
    @Size(max = 4000) String observacoes,
    /** Para reentrega: id da entrega original (gera nova entrega ligada). */
    UUID entregaOriginalId) {

  /** Quando vem de ProximaEntrega, exige só o id; senão, exige os 4 campos. */
  public boolean veioDeProximaEntrega() {
    return proximaEntregaId != null;
  }

  public void validar() {
    if (veioDeProximaEntrega()) return;
    if (clienteId == null || produtoId == null || releaseId == null || ambiente == null) {
      throw new IllegalArgumentException(
          "Quando não vem de proximaEntregaId, clienteId, produtoId, releaseId e ambiente são obrigatórios.");
    }
  }
}
