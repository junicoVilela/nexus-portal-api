package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfigEntregaRequest(
    @NotNull TipoDestinoEntrega tipoDestino,
    @Size(max = 500) String caminhoBase,
    Boolean exigirAprovacao,
    @Size(max = 1000) String emailsNotificacao) {}
