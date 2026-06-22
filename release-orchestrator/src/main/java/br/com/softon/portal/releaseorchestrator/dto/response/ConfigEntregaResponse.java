package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.ConfigEntrega;
import br.com.softon.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConfigEntregaResponse(
    UUID id,
    UUID clienteId,
    TipoDestinoEntrega tipoDestino,
    String caminhoBase,
    boolean exigirAprovacao,
    String emailsNotificacao,
    String host,
    Integer porta,
    String usuario,
    /** True quando há senha cadastrada; o valor cifrado nunca é devolvido. */
    boolean senhaConfigurada,
    Boolean modoPassivo,
    Boolean strictHostCheck,
    OffsetDateTime updatedAt) {

  public static ConfigEntregaResponse from(ConfigEntrega c) {
    return new ConfigEntregaResponse(
        c.getId(),
        c.getCliente().getId(),
        c.getTipoDestino(),
        c.getCaminhoBase(),
        c.isExigirAprovacao(),
        c.getEmailsNotificacao(),
        c.getHost(),
        c.getPorta(),
        c.getUsuario(),
        c.temSenhaConfigurada(),
        c.getModoPassivo(),
        c.getStrictHostCheck(),
        c.getUpdatedAt());
  }
}
